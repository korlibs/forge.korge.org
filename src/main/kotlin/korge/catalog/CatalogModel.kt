package korge.catalog

import korge.*
import korlibs.datastructure.*
import korlibs.io.dynamic.*
import korlibs.io.serialization.yaml.*
import java.io.*
import java.net.*

class CatalogModel(val info: Dyn) {
    val installers = info["installers"].map.map {
        val key = it.key.str
        val info = it.value["name"].toStringOrNull() ?: key
        val version = it.value["version"].toStringOrNull() ?: "2024.unknown"
        val actions = it.value["actions"].list.map { it.str }
        Installer(info, version, actions)
    }

    val actionGroups = info["actions"].map.map {
        val groupName = it.key.str
        ActionGroup(groupName, it.value.list.mapIndexed { index, it -> parseAction(it, index, groupName) })
    }.associateBy { it.name }

    inner class Installer(val name: String, val version: String, val actionGroups: List<String>) : Extra by Extra() {
        val model get() = this@CatalogModel

        override fun toString(): String = name
    }

    inner class ActionGroup(val name: String, val actions: List<SimpleAction>) : Extra by Extra() {
        val model get() = this@CatalogModel
    }

    inner class SimpleAction(
        val index: Int,
        val groupName: String,
        val downloads: Downloads,
        val localFile: String? = null,
        val filter: String? = null,
        val extract: String? = null,
        val copy: String? = null,
        val name: String = "$groupName:$index",
        val createShortcut: String? = null,
    ) : Extra by Extra() {
        val model get() = this@CatalogModel
    }

    inner class Downloads(val downloads: Map<String, Download> = emptyMap()) {
        val model get() = this@CatalogModel

        val name: String get() = downloads.values.filter { it.matches() }.joinToString(", ") { it.baseName }
    }

    inner class Download(val type: String, val url: String, val sha256: String?) {
        val typeParts = type.split(".").filter { it != "download" }
        var os: OS? = typeParts.firstNotNullOfOrNull { OS[it] }
        var arch: ARCH? = typeParts.firstNotNullOfOrNull { ARCH[it] }
        fun matches(os: OS = OS.CURRENT, arch: ARCH = ARCH.CURRENT): Boolean = (this.os == os || this.os == null)
            && (this.arch == arch || this.arch == null)
        val baseName get() = File(URL(url).path).name

        override fun toString(): String = "Download(type=$type, os=$os, arch=$arch, url=$url, sha256=$sha256)"
    }

    fun parseAction(it: Dyn, index: Int, groupName: String): SimpleAction {
        val downloads = LinkedHashMap<String, Download>()

        for ((key, value) in it.map) {
            val keyStr = key.str
            val valueStr = value.str
            if (!keyStr.startsWith("download")) continue
            val parts = valueStr.split("::")
            downloads[keyStr] = Download(keyStr, parts.first(), parts.getOrNull(1))
        }

        return SimpleAction(
            index = index,
            groupName = groupName,
            downloads = Downloads(downloads),
            localFile = it["local_file"].toStringOrNull(),
            filter = it["filter"].toStringOrNull(),
            extract = it["extract"].toStringOrNull(),
            copy = it["copy"].toStringOrNull(),
            createShortcut = it["create_shortcuts"].toStringOrNull(),
        )
    }

    companion object {
        val DEFAULT by lazy {
            load()
        }
        fun load(): CatalogModel {
            return CatalogModel(Yaml.read(CatalogModel::class.java.getResource("/catalog.yaml")?.readText() ?: error("Can't find catalog.yaml")).dyn)
        }
    }
}
