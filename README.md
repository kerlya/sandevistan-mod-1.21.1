# Sandevistan Fabric 1.21.1

Требования: Java 21, Minecraft 1.21.1, Fabric Loader 0.16.5, Fabric API 0.104.0+1.21.1.

## Сборка
Windows: `gradlew.bat build`
Linux/macOS: `chmod +x gradlew && ./gradlew build`
JAR: `build/libs/sandevistan-1.0.0.jar`

Скрипты gradlew/gradlew.bat являются bootstrap-обёртками: скачивают Gradle 8.10.2 при первом запуске.

## GitHub Actions
Загрузите проект в GitHub. Вкладка Actions → Build Fabric mod → Run workflow. JAR будет доступен в Artifacts.

## Звуки
Замените `src/main/resources/assets/sandevistan/sounds/activate.ogg` и `smasher_clash.ogg` файлами OGG Vorbis, сохранив имена.
