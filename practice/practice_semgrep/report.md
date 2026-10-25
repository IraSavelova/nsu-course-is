
## 1. Описание внедренной уязвимости

https://github.com/IraSavelova/nsu-course-is/tree/main/practice/practice1

- **Тип уязвимости:** OS Command Injection
- **CWE:** CWE-78
- **Файл и строки:** /Users/irina/nsu-course-is/practice/practice1/demo/demoapplication/src/main/java/nsu/security/demoapplication/controller , строки 93-113

- **Источник (откуда приходят недоверенные данные):**  параметр `filename` из `@RequestParam`
- **Синк (где данные используются опасно):** `new ProcessBuilder("sh", "-c", command);`

Фрагмент уязвимого кода:

```java
String command = "convert " + UPLOAD_DIR + "/" + filename

+ " " + UPLOAD_DIR + "/converted.jpg";

ProcessBuilder processBuilder = new ProcessBuilder(

"sh", "-c", command

);
```

Кратко опишите исправление:

```java

ProcessBuilder processBuilder = new ProcessBuilder(

"convert",

inputFile.toString(),

outputFile.toString()

);
```

## 2. Параметры запуска

| Параметр      | Значение                                |
| ------------- | --------------------------------------- |
| Сканер        | **semgrep**                             |
| Наборы правил | `p/java, p/owasp-top-ten, p/dockerfile` |

Команда запуска:

```bash
semgrep scan \
  --config p/java \
  --config p/owasp-top-ten \
  --config p/dockerfile \
  --json \
  --output semgrep-report.json .
```

Проверка покрытия: файл с уязвимостью присутствует в списке просканированных файлов, ошибок парсинга нет. 

Приложенные отчеты сканера:

- Уязвимая/исправленная версия: '/Users/irina/nsu-course-is/practice/practice1/demo/semgrep-report.json'

## 3. Результаты сканирования

| Версия       | Результат | Правило                                                                        |
| ------------ | --------- | ------------------------------------------------------------------------------ |
| Уязвимая     | TP        | `java.spring.security.injection.tainted-system-command.tainted-system-command` |
| Исправленная | TN        | -                                                                              |

### 3.1. Уязвимая версия

Фрагмент находки:

| Поле              | Значение                                                                                                                                                                                                       |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ID правила        | `java.spring.security.injection.tainted-system-command.tainted-system-command`                                                                                                                                 |
| CWE               | CWE-78  OS Command Injection                                                                                                                                                                                   |
| Файл              | `demoapplication/src/main/java/nsu/security/demoapplication/controller/FileController.java`                                                                                                                    |
| Строка            | 103                                                                                                                                                                                                            |
| Сообщение сканера | Detected user input entering a method which executes a system command. This could result in a command injection vulnerability, which allows an attacker to inject an arbitrary system command onto the server. |
Находка соответствует реальной уязвимости в проверяемом коде, поэтому была классифицирована как TP. Semgrep обнаружил уязвимость: имя файла, которое вводит пользователь, добавляется к системной команде через конкатенацию строк. Затем команда передаётся в `sh -c`, из-за чего специальные символы в имени файла могут привести к выполнению посторонних команд. Следовательно, находка соответствует CWE-78. 

### 3.2. Исправленная версия

True Negative.
В исправленном методе convertFileSafe() команда и её аргументы передаются в `ProcessBuilder` отдельно, без использования `sh -c`. Это исключает выполнение каких-либо посторонних программ через имя файла. Semgrep тут не обнаружил уязвимость. 

## 4. Анализ результатов

### 4.1. Как сработало правило (для TP)

- **Источник в правиле:**  параметр `filename`, который пользователь передаёт в запросе через `@RequestParam`
- **Синк в правиле:** ProcessBuilder("sh", "-c", command).
- **Соответствие коду:** 103

## 6. Выводы
Сканер успешно обнаружил уязвимость OS Command Injection (CWE-78) в методе `convertFile()`, где пользовательский ввод используется через конкатенацию строк и выполняется с помощью `sh -c`. Исправленный метод `convertFileSafe()` передаёт команду и аргументы отдельно, что предотвращает выполнение посторонних команд через имя файла. 
