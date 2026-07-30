fun main() {
    var spokenText = "Here is what I found. <SEARCH>hello</SEARCH> and <SEARCH>world</SEARCH>. Done."
    var hasCommand = false
           
    val searchRegex = "<SEARCH>(.*?)</SEARCH>".toRegex(RegexOption.DOT_MATCHES_ALL)
    searchRegex.findAll(spokenText).forEach {
        println("Found: ${it.groupValues[1]}")
        spokenText = spokenText.replace(it.value, "")
        hasCommand = true
    }
    println("Spoken text: $spokenText")
}
