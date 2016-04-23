# asl-service

Instrukcja instalacji:
1. zainstalowac pakiet lm-sensors
2. sudo sensors-detect (wszedzie zaakceptowac)
3. zainstalowac pakiet ifstat
4. zainstalowac pakiet iostat
5. zainstalowac serwer bazy danych
6. odpalic skrypt ./src/main/resources/create_db.sql
7. dodac plik asl-service.properties do katalogu ./src/main/resources z parametrami:
    * db.url - url bazy danych
    * db.username - uzytkownik bazy danych
    * db.password - haslo podanego uzytkownika do bazy danych

    Przykladowy plik konfiguracyjny:
    -----
    db.url=jdbc:postgresql://localhost:5432/db
    db.username=rmartyna
    db.password=rmartyna
    -----
8. uruchomic asl-service z klasy Main



