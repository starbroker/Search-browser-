sed -i "s/val textToType = .*/val textToType = arg.replace(\"\\\"\", \"\\\\\\\\\\\"\").replace(\"'\", \"\\\\\\\\'\")/g" app/src/main/java/com/example/ui/AICommandHandler.kt
sed -i "s/val textToClick = .*/val textToClick = arg.replace(\"\\\"\", \"\\\\\\\\\\\"\").replace(\"'\", \"\\\\\\\\'\")/g" app/src/main/java/com/example/ui/AICommandHandler.kt
