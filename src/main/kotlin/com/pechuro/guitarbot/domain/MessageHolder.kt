package com.pechuro.guitarbot.domain

import com.pechuro.guitarbot.app.Configuration
import com.pechuro.guitarbot.data.DataRepository
import com.pechuro.guitarbot.ext.getStringFromResources
import com.pechuro.guitarbot.ext.plusNotNull
import java.util.concurrent.atomic.AtomicLong

private val idGenerator = AtomicLong()

private val escapeRegex = """[_*\[\]()~`>#+\-=|{}.!]""".toRegex()
val String.escaped: String
    get() = escapeRegex.replace(this, "\\\\$0")

private const val MAX_TEXT_LENGTH = 4096

class MessageHolder(private val repository: DataRepository) {

    val rootMessage: BotMessage.Content = BotMessage.Content(
        id = idGenerator.getAndIncrement(),
        label = "",
        text = getStringFromResources("message.main").escaped,
        parent = null,
        nodes = emptyList()
    )

    private val searchCache = mutableMapOf<Long, BotMessage>()

    fun startSearch(predicate: String, chatId: Long): BotMessage.Content {
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

    fun findMessage(messageInfo: MessageInfo): BotMessage? {
        val rootNode = when (messageInfo) {
            is MessageInfo.Normal -> rootMessage
            is MessageInfo.Search -> searchCache[messageInfo.chatId]
        } ?: return null
        return findMessageByIdInternal(messageInfo.id, rootNode)
    }

    fun loadMessages(id: String = "", node: BotMessage.Content = rootMessage) {
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
                    loadMessages(data.id, root)
                }
            }
            .plusNotNull(node.parent?.let { createBackMessage(it) })
        val lastNode = remoteDataList.addText(node)
        remoteDataList.addFiles(lastNode)
    }

    private fun findMessageByIdInternal(id: Long, node: BotMessage = rootMessage): BotMessage? {
        if (node.id == id) return node
        if (node is BotMessage.Content) {
            node.nodes.forEach {
                return findMessageByIdInternal(id, it) ?: return@forEach
            }
        }
        return null
    }

    private fun List<RemoteData>.addText(parentNode: BotMessage.Content): BotMessage.Content = this
        .filterIsInstance<RemoteData.Text>()
        .sortedBy { it.page }
        .flatMap { it.text.chunked(MAX_TEXT_LENGTH - parentNode.text.length) }
        .map { "${parentNode.text}\n\n$it" }
        .createNodes(parentNode)

    private fun List<RemoteData>.addFiles(parentNode: BotMessage.Content) = this
        .filterIsInstance<RemoteData.File>()
        .chunked(Configuration.App.maxFilesPerPage)
        .map { files -> getTextForFiles(base = parentNode.text, files = files) }
        .flatMap { it.chunked(MAX_TEXT_LENGTH - parentNode.text.length) }
        .createNodes(parentNode)

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
    }.let { nodes ->
        for (i in nodes.indices) {
            nodes[i].nodes = listOfNotNull(nodes.getOrNull(i + 1))
                .plus(nodes[i].nodes)
                .sorted()
        }
        nodes.lastOrNull() ?: parentNode
    }

    private fun List<BotMessage>.sorted() = sortedBy {
        when (it.label) {
            getStringFromResources("action.previousPage") -> 1
            getStringFromResources("action.nextPage") -> 2
            getStringFromResources("action.back") -> 4
            else -> 3
        }
    }

    private fun getTextForFiles(base: String, files: List<RemoteData.File>) = buildString {
        append(base)
        if (files.isNotEmpty()) {
            append("\n\n${files.getFormattedFiles()}")
        }
    }

    private fun List<RemoteData.File>.getFormattedFiles(): String {
        return joinToString(separator = "") { "• [${it.name.escaped}](${it.url})\n" }
    }

    private fun createBackMessage(parent: BotMessage) = BotMessage.Back(parent = parent)
}