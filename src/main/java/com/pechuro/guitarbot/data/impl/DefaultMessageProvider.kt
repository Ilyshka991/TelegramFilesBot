package com.pechuro.guitarbot.data.impl

import com.pechuro.guitarbot.app.Configuration
import com.pechuro.guitarbot.data.DataRepository
import com.pechuro.guitarbot.data.RemoteData
import com.pechuro.guitarbot.domain.BotMessage
import com.pechuro.guitarbot.domain.BotMessageProvider
import com.pechuro.guitarbot.domain.BotMessageInfo
import com.pechuro.guitarbot.ext.getStringFromResources
import com.pechuro.guitarbot.ext.plusNotNull
import java.util.concurrent.atomic.AtomicLong

private val idGenerator = AtomicLong()

private val escapeRegex = """[_*\[\]()~`>#+\-=|{}.!]""".toRegex()
val String.escaped: String
    get() = escapeRegex.replace(this, "\\\\$0")

private const val MAX_TEXT_LENGTH = 4096

class DefaultMessageProvider(private val repository: DataRepository) : BotMessageProvider {

    override val rootMessage: BotMessage.Content = BotMessage.Content(
        id = idGenerator.getAndIncrement(),
        label = "",
        text = getStringFromResources("message.main").escaped,
        parent = null,
        nodes = emptyList()
    )

    private val searchCache = mutableMapOf<Long, BotMessage>()

    init {
        sync()
    }

    override fun search(chatId: Long, predicate: String): BotMessage.Content {
        val files = repository.findFiles(predicate)
        val textResId = if (files.isEmpty()) "message.notFound" else "message.found"
        val parentNode = BotMessage.Content(
            id = idGenerator.getAndIncrement(),
            label = getStringFromResources("action.find").escaped,
            text = getStringFromResources(textResId).escaped,
            parent = null,
            nodes = emptyList()
        )
        files.addFiles(parentNode)
        searchCache[chatId] = parentNode
        return parentNode
    }

    override fun get(messageInfo: BotMessageInfo): BotMessage.Content? {
        val rootNode = when (messageInfo) {
            is BotMessageInfo.Normal -> rootMessage
            is BotMessageInfo.Search -> searchCache[messageInfo.chatId]
        } ?: return null
        return findMessageByIdInternal(messageInfo.id, rootNode)
    }

    override fun sync() {
        loadRemoteMessages(
            id = "",
            node = rootMessage
        )
    }

    private fun loadRemoteMessages(id: String, node: BotMessage.Content) {
        val remoteDataList = repository.getBySourceId(id)
        node.nodes = remoteDataList
            .filterIsInstance<RemoteData.Folder>()
            .map { data ->
                BotMessage.Content(
                    id = idGenerator.getAndIncrement(),
                    label = data.name,
                    text = getStringFromResources("message.selectedText").format(data.name.escaped),
                    parent = node,
                    nodes = emptyList()
                ).also { root ->
                    loadRemoteMessages(data.id, root)
                }
            }
            .plusNotNull(node.parent?.let { createBackMessage(it) })
        val lastNode = remoteDataList.addText(node)
        remoteDataList.addFiles(lastNode)
    }

    private fun findMessageByIdInternal(id: Long, node: BotMessage = rootMessage): BotMessage.Content? = when {
        node !is BotMessage.Content -> null
        node.id == id -> node
        else -> {
            node.nodes.forEach {
                return findMessageByIdInternal(id, it) ?: return@forEach
            }
            null
        }
    }

    private fun List<RemoteData>.addText(parentNode: BotMessage.Content): BotMessage.Content = this
        .filterIsInstance<RemoteData.Text>()
        .sortedBy { it.page }
        .flatMap { it.text.chunked(MAX_TEXT_LENGTH - parentNode.text.length) }
        .map { "${parentNode.text}\n\n$it" }
        .createNodes(parentNode)
        .connectNodes()
        .lastOrNull() ?: parentNode

    private fun List<RemoteData>.addFiles(parentNode: BotMessage.Content) = this
        .filterIsInstance<RemoteData.File>()
        .chunked(Configuration.App.MAX_FILES_PER_PAGE)
        .map { it.mapToText(base = parentNode.text) }
        .flatMap { it.chunked(MAX_TEXT_LENGTH - parentNode.text.length) }
        .createNodes(parentNode)
        .connectNodes()
        .lastOrNull() ?: parentNode

    private fun List<String>.createNodes(parentNode: BotMessage.Content) = foldIndexed(
        mutableListOf<BotMessage.Content>()
    ) { index, nodes, text ->
        if (index == 0) {
            parentNode.text = text
            nodes.add(parentNode)
        } else {
            val backNode = BotMessage.Back(
                label = getStringFromResources("action.previousPage"),
                parent = nodes[index - 1]
            )
            val nextNode = BotMessage.Content(
                id = idGenerator.getAndIncrement(),
                label = getStringFromResources("action.nextPage"),
                parent = parentNode,
                text = text,
                nodes = listOf(backNode).plus(parentNode.nodes)
            )
            nodes.add(nextNode)
        }
        nodes
    }

    private fun List<BotMessage.Content>.connectNodes() = apply {
        for (i in indices) {
            this[i].nodes = listOfNotNull(getOrNull(i + 1)).plus(this[i].nodes).sorted()
        }
    }

    private fun List<BotMessage>.sorted() = sortedBy {
        when (it.label) {
            getStringFromResources("action.previousPage") -> 1
            getStringFromResources("action.nextPage") -> 2
            getStringFromResources("action.back") -> 4
            else -> 3
        }
    }

    private fun List<RemoteData.File>.mapToText(base: String) = buildString {
        append(base)
        if (isNotEmpty()) {
            val files = joinToString(separator = "") { it.formatFileName() }
            append("\n\n${files}")
        }
    }

    private fun RemoteData.File.formatFileName() = "• [${name.escaped}](${url})\n"

    private fun createBackMessage(parent: BotMessage.Content) = BotMessage.Back(parent = parent)
}