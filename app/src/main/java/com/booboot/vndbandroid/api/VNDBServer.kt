package com.booboot.vndbandroid.api

import com.booboot.vndbandroid.App
import com.booboot.vndbandroid.BuildConfig
import com.booboot.vndbandroid.R
import com.booboot.vndbandroid.extensions.log
import com.booboot.vndbandroid.model.vndb.DbStats
import com.booboot.vndbandroid.model.vndb.Error
import com.booboot.vndbandroid.model.vndb.Fields
import com.booboot.vndbandroid.model.vndb.Login
import com.booboot.vndbandroid.model.vndb.Options
import com.booboot.vndbandroid.model.vndb.Response
import com.booboot.vndbandroid.model.vndb.Results
import com.booboot.vndbandroid.model.vndbandroid.Preferences
import com.booboot.vndbandroid.util.ErrorHandler
import com.booboot.vndbandroid.util.Logger
import com.booboot.vndbandroid.util.TypeReference
import com.booboot.vndbandroid.util.type
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.SocketException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.math.max
import kotlin.math.min

class VNDBServer @Inject constructor(private val moshi: Moshi) {
    private fun connect(socketIndex: Int) = try {
        val sf = SSLSocketFactory.getDefault()
        val socket = sf.createSocket(HOST, PORT) as SSLSocket
        socket.keepAlive = false
        socket.soTimeout = 0

        val hv = HttpsURLConnection.getDefaultHostnameVerifier()
        val s = socket.session

        if (!hv.verify(HOST, s)) {
            throw Throwable("The API's certificate is not valid. Expected $HOST")
        }

        SocketPool.setSocket(socketIndex, socket)
    } catch (uhe: UnknownHostException) {
        throw Throwable("Unable to reach the server $HOST. Please try again later.")
    } catch (ioe: IOException) {
        throw Throwable("An error occurred during the connection to the server. Please try again later.")
    }

    /**
     * Logs the user with its credentials to VNDB.org. Connects the SSL socket then send a login request.
     * So it can be automatic, this is never called manually, but always internally by SocketPool, on behalf of another request.
     */
    fun login(options: Options) {
        synchronized(SocketPool.getLock(options.socketIndex)) {
            if (SocketPool.getSocket(options.socketIndex) == null) {
                connect(options.socketIndex)
                val username = Preferences.username?.toLowerCase()?.trim() ?: ""
                val password = Preferences.password ?: ""
                val login = Login(PROTOCOL, CLIENT, CLIENTVER, username, password)
                sendCommand("login " + moshi.adapter(Login::class.java).toJson(login), options, type<Response<Void>>())
            }
        }
    }

    fun <T> get(type: String, flags: String, filters: String, options: Options = Options(), resultClass: TypeReference<Results<T>>): Single<Results<T>> {
        val command = "get $type $flags $filters "

        return if (options.numberOfPages > 1) {
            options.numberOfSockets = max(min(options.numberOfPages / 2, SocketPool.MAX_SOCKETS), 3)

            /* We know in advance how many pages we must fetch: we can parallelize them with Single.merge */
            val observables = mutableListOf<Single<Response<Results<T>>>>()
            (0 until options.numberOfPages).mapTo(observables) { index ->
                val socketIndex = index % options.numberOfSockets
                Single.fromCallable<Response<Results<T>>> {
                    val threadOptions = options.copy(page = index + 1, socketIndex = socketIndex)
                    sendCommand(command + moshi.adapter(Options::class.java).toJson(threadOptions), threadOptions, resultClass)
                }.doOnError { VNDBServer.close(socketIndex) }.subscribeOn(Schedulers.newThread())
            }

            Single.merge(observables).collect({ Results<T>() }, { results, response ->
                results.items.addAll(response.results?.items ?: emptyList())
            })
        } else {
            /* We don't know how many pages we must fetch (hence how many requests we have to send): creating Singles sequentially until done */
            Single.fromCallable<Results<T>> {
                val results = Results<T>()
                do {
                    options.socketIndex %= options.numberOfSockets
                    val response = sendCommand(command + moshi.adapter(Options::class.java).toJson(options), options, resultClass)
                    results.items.addAll(response.results?.items ?: emptyList())
                    options.page++
                } while (options.fetchAllPages && response.results?.more == true)

                results
            }
                .doOnError { VNDBServer.close(options.socketIndex) }
                .subscribeOn(Schedulers.newThread())
        }
    }

    fun set(type: String, id: Int, fields: Fields): Completable {
        val command = "set $type $id " + moshi.adapter(Fields::class.java).toJson(fields)

        return Single.fromCallable<Response<Void>> {
            sendCommand(command, resultClass = type())
        }.flatMapCompletable { Completable.complete() }
    }

    @Suppress("unused")
    fun dbstats(): Single<DbStats> {
        return Single.fromCallable<Response<DbStats>> {
            sendCommand("dbstats", resultClass = type())
        }.map { response: Response<DbStats> -> response.results!! }
    }

    private fun <T> sendCommand(command: String, options: Options = Options(), resultClass: TypeReference<T>, retry: Boolean = false): Response<T> =
        synchronized(SocketPool.getLock(options.socketIndex)) {
            val query = command + EOM
            var response: Response<T>?

            try {
                val socket = SocketPool.getSocket(this, options) ?: throw Throwable("Unable to connect. Please try again later.")
                val output = socket.outputStream
                val input = InputStreamReader(socket.inputStream)

                do {
                    if (BuildConfig.DEBUG) Logger.log(query)
                    if (input.ready()) while (input.read() > -1);
                    output.flush()
                    output.write(query.toByteArray(charset("UTF-8")))

                    var isThrottled = false
                    response = getResponse(input, resultClass)
                    response?.error?.let { error ->
                        isThrottled = error.id == "throttled"
                        if (isThrottled) {
                            if (SocketPool.throttleHandlingSocket < 0) SocketPool.throttleHandlingSocket = options.socketIndex

                            /* We got throttled by the server. Displaying a warning message */
                            val fullwait = Math.round(error.fullwait / 60)
                            ErrorHandler.showToast(String.format(App.context.getString(R.string.throttle_warning), fullwait))
                            try {
                                /* Waiting ~minwait if the we are in the thread that handles the throttle, 5+ minwaits otherwise */
                                val waitingFactor = (if (SocketPool.throttleHandlingSocket == options.socketIndex) 1050 else 5000 + 500 * options.socketIndex).toLong()
                                Thread.sleep(Math.round(error.minwait * waitingFactor))
                            } catch (e: InterruptedException) {
                                /* For some reason we weren't able to sleep, so we can ignore the exception and keep rolling anyway */
                            }
                        } else {
                            throw Throwable(error.fullMessage())
                        }
                    }
                } while (isThrottled)
            } catch (uee: UnsupportedEncodingException) {
                throw Throwable("Tried to send a query to the API with a wrong encoding. Aborting operation.")
            } catch (se: SocketException) {
                throw Throwable("A connection error occurred. Please check your connection or try again later.")
            } catch (ssle: SSLException) {
                VNDBServer.close(options.socketIndex)
                return if (retry || command.startsWith("login", true)) {
                    throw Throwable("An error occurred while writing a query to the API. Please try again later.")
                } else {
                    sendCommand(command, options, resultClass, true)
                }
            } catch (ioe: IOException) {
                throw Throwable("An error occurred while sending a query to the API. Please try again later.")
            } catch (t: Throwable) {
                throw t
            } finally {
                if (SocketPool.throttleHandlingSocket > -1 && SocketPool.throttleHandlingSocket == options.socketIndex)
                    SocketPool.throttleHandlingSocket = -1
            }

            response ?: throw Throwable("Empty response.")
        }

    private fun <T> getResponse(input: InputStreamReader, resultClass: TypeReference<T>): Response<T>? {
        val responseWrapper = Response<T>()
        val response = StringBuilder()
        try {
            var read = input.read()
            while (read != 4 && read > -1) {
                response.append(read.toChar())
                read = input.read()
            }
        } catch (exception: Exception) {
            throw Throwable("An error occurred while receiving the response from the API. Please try again later.")
        }

        //        if (BuildConfig.DEBUG) Logger.log(response.toString())

        val delimiterIndex = response.indexOf("{")
        if (delimiterIndex < 0) {
            return if (response.toString().trim() == "ok") {
                responseWrapper.ok = true
                responseWrapper
            } else {
                /* Undocumented error : the server returned an empty response (""), which means absolutely nothing but "leave the ship because something undebuggable happened!" */
                throw Throwable("VNDB.org returned an unexpected error. Please try again later.")
            }
        }

        return try {
            val command = response.substring(0, delimiterIndex).trim()
            val params = response.substring(delimiterIndex, response.length).replace(EOM + "", "")
            if (command == "error") {
                responseWrapper.error = moshi.adapter(Error::class.java).fromJson(params)
            } else {
                responseWrapper.results = moshi.adapter<T>(resultClass.type).fromJson(params)
            }
            responseWrapper
        } catch (ioe: IOException) {
            throw Throwable("An error occurred while decoding the response from the API. Aborting operation.")
        }
    }

    companion object {
        private const val HOST = "api.vndb.org"
        private const val PORT = 19535
        private const val EOM: Char = 0x04.toChar()

        private const val PROTOCOL = 1
        const val CLIENT = "VNDB_ANDROID"
        private const val CLIENTVER = 3.0

        fun close(socketIndex: Int) {
            try {
                SocketPool.getSocket(socketIndex)?.let {
                    it.inputStream.close()
                    it.outputStream.close()
                    it.close()
                    SocketPool.setSocket(socketIndex, null)
                }
            } catch (ioe: IOException) {
                ioe.log()
            }
        }

        fun closeAll(): Completable = Completable.fromAction { for (i in 0 until SocketPool.MAX_SOCKETS) close(i) }
            .subscribeOn(Schedulers.io())
    }
}