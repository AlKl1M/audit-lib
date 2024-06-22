# auditlog-spring-boot-autoconfigure

Содержит всё неободимое для начала работы с библиотекой.

### Конфигурация логгирования
В вашем приложении для указания способов логгирования методов auditlog требуется указать файл конфигурации.
Вы можете написать свой, но библиотека предоставляет следующие конфигурационные файлы:

- auditlog-log4j2-default.xml логгирует методы auditlog в консоль.
- auditlog-log4j2-file.xml логгирует методы auditlog в файл.
- auditlog-log4j2-file-console.xml логгирует методы auditlog в файл и консоль.

Пример:

```yaml
logging:
config: classpath:log4j2/auditlog-log4j2-file.xml
```