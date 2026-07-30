import java.io.File
import java.util.Base64

fun main() {
    val file = File("app/src/main/java/com/example/ui/AICommandHandler.kt")
    var content = file.readText()
    
    val regex = Regex("val textToType = .*")
    content = content.replace(regex, "val textToType = arg.replace(\"\\\"\", \"\\\\\\\"\").replace(\"'\", \"\\\\\")")
    
    val regex2 = Regex("val textToClick = .*")
    content = content.replace(regex2, "val textToClick = arg.replace(\"\\\"\", \"\\\\\\\"\").replace(\"'\", \"\\\\'\")")
    
    file.writeText(content)
}
