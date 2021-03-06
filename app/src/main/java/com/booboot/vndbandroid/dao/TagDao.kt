package com.booboot.vndbandroid.dao

import com.booboot.vndbandroid.model.vndb.Tag
import io.objectbox.BoxStore
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.kotlin.boxFor
import io.objectbox.relation.ToMany

@Entity
class TagDao() {
    @Id(assignable = true) var id: Long = 0
    var name: String = ""
    var description: String = ""
    var meta: Boolean = false
    var vns: Int = 0
    var cat: String = ""
    lateinit var aliases: ToMany<TagAlias>
    lateinit var parents: ToMany<TagParent>

    constructor(tag: Tag, boxStore: BoxStore) : this() {
        id = tag.id
        name = tag.name
        description = tag.description
        meta = tag.meta
        vns = tag.vns
        cat = tag.cat

        boxStore.boxFor<TagDao>().attach(this)
        tag.aliases.forEach { aliases.add(TagAlias(it.hashCode().toLong(), it)) }
        tag.parents.forEach { parents.add(TagParent(it)) }

        boxStore.boxFor<TagAlias>().put(aliases)
        boxStore.boxFor<TagParent>().put(parents)
    }

    fun toBo() = Tag(
        id,
        name,
        description,
        meta,
        vns,
        cat,
        aliases.map { it.alias },
        parents.map { it.id }
    )
}

@Entity
data class TagAlias(
    @Id(assignable = true) var id: Long = 0,
    var alias: String = ""
)

@Entity
data class TagParent(
    @Id(assignable = true) var id: Long = 0
)