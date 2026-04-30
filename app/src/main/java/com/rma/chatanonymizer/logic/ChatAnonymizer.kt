package com.rma.chatanonymizer.logic

import java.util.regex.Matcher
import java.util.regex.Pattern

class ChatAnonymizer(
    private val maskUrls: Boolean = true,
    private val removeTimestamps: Boolean = true,
    private val participantPrefix: String = "Person",
    private val phonePlaceholder: String = "[PHONE %d]",
    private val emailPlaceholder: String = "[EMAIL]",
    private val urlPlaceholder: String = "[URL]"
) {

    // Identifica o início da linha com Data e Hora
    private val androidBasePattern =
        Pattern.compile("^(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}),\\s(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s?[ap]m)?)\\s-\\s(.*)$")
    private val iosBasePattern =
        Pattern.compile("^\\[(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}),\\s(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s?[AP]M)?)\\]\\s(.*)$")

    // Procura o separador de remetente ": "
    private val messageContentPattern = Pattern.compile("^([^:]+):\\s(.*)$")

    private val phonePattern =
        Pattern.compile("\\+?\\d{1,4}?[-.\\s]?\\(?\\d{1,3}?\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}")
    private val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val urlPattern = Pattern.compile("https?://\\S+|www\\.\\S+")

    private val identityMap = mutableMapOf<String, Int>()
    private var participantCount = 0

    fun anonymize(lines: List<String>): List<String> {
        identityMap.clear()
        participantCount = 0
        discoverParticipants(lines)
        return transformLines(lines)
    }

    private fun discoverParticipants(lines: List<String>) {
        for (line in lines) {
            val baseMatcher = getBaseMatcher(line)
            if (baseMatcher != null) {
                val remaining = baseMatcher.group(3) ?: continue
                val contentMatcher = messageContentPattern.matcher(remaining)
                if (contentMatcher.find()) {
                    val rawIdentifier = contentMatcher.group(1) ?: continue
                    getParticipantIndex(rawIdentifier)
                }
            }
        }
    }

    private fun getParticipantIndex(identifier: String): Int {
        val cleaned = identifier.replace("~", "").replace("\u202f", " ").trim()
        val normalized = if (phonePattern.matcher(cleaned).matches()) {
            cleaned.replace("\\s|-|\\(|\\)".toRegex(), "")
        } else cleaned

        return identityMap.getOrPut(normalized) {
            participantCount++
            participantCount
        }
    }

    private fun getBaseMatcher(line: String): Matcher? {
        val androidMatcher = androidBasePattern.matcher(line)
        if (androidMatcher.find()) return androidMatcher
        val iosMatcher = iosBasePattern.matcher(line)
        if (iosMatcher.find()) return iosMatcher
        return null
    }

    private fun transformLines(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var currentParticipantIndex: Int? = null

        for (line in lines) {
            val baseMatcher = getBaseMatcher(line)
            if (baseMatcher != null) {
                val remaining = baseMatcher.group(3) ?: ""
                val contentMatcher = messageContentPattern.matcher(remaining)

                if (contentMatcher.find()) {
                    // É uma mensagem de usuário (possui ": ")
                    val rawIdentifier = contentMatcher.group(1) ?: ""
                    val content = contentMatcher.group(2) ?: ""
                    val index = getParticipantIndex(rawIdentifier)
                    currentParticipantIndex = index

                    val prefix = if (!removeTimestamps) {
                        "${baseMatcher.group(1)}, ${baseMatcher.group(2)} - "
                    } else ""

                    result.add("${prefix}${participantPrefix} $index: ${anonymizeContent(content)}")
                } else {
                    // Tem data mas não tem ": ", então é MENSAGEM DE SISTEMA. Ignoramos.
                    currentParticipantIndex = null
                    continue
                }
            } else {
                // Continuação de mensagem multilinhas
                if (currentParticipantIndex != null) {
                    result.add(anonymizeContent(line))
                }
            }
        }
        return result
    }

    private fun anonymizeContent(content: String): String {
        var anonymized = content
        val phoneMatcher = phonePattern.matcher(anonymized)
        val sb = StringBuilder()
        var lastEnd = 0
        while (phoneMatcher.find()) {
            sb.append(anonymized.substring(lastEnd, phoneMatcher.start()))
            val foundPhone = phoneMatcher.group().replace("\\s|-|\\(|\\)".toRegex(), "")
            val index = identityMap[foundPhone] ?: getParticipantIndex(foundPhone)
            sb.append(phonePlaceholder.format(index))
            lastEnd = phoneMatcher.end()
        }
        sb.append(anonymized.substring(lastEnd))
        anonymized = sb.toString()

        anonymized = emailPattern.matcher(anonymized).replaceAll(emailPlaceholder)
        if (maskUrls) anonymized = urlPattern.matcher(anonymized).replaceAll(urlPlaceholder)

        val mentionPattern = Pattern.compile("@(\\S+)")
        val mentionMatcher = mentionPattern.matcher(anonymized)
        val mentionSb = StringBuilder()
        var mentionLastEnd = 0
        while (mentionMatcher.find()) {
            mentionSb.append(anonymized.substring(mentionLastEnd, mentionMatcher.start()))
            val target = mentionMatcher.group(1) ?: ""
            val index =
                identityMap[target] ?: identityMap[target.replace("\\s|-|\\(|\\)".toRegex(), "")]
            mentionSb.append(if (index != null) "@$participantPrefix$index" else "@$participantPrefix")
            mentionLastEnd = mentionMatcher.end()
        }
        mentionSb.append(anonymized.substring(mentionLastEnd))
        return mentionSb.toString()
    }
}