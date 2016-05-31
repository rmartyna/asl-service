# asl-service

Wymagania:
- java w wersji 8
- system zarządzania pakietami(np. apt, yum, packman)

Instrukcja instalacji:

1. Zainstalować pakiet lm-sensors

2. sudo sensors-detect (wszędzie zaakceptować)

3. Zainstalować pakiet ifstat

4. Zainstalować pakiet iostat

5. Skopiować plik asl-service.jar do katalogu {INSTALL_DIR}

6. Utworzyć plik asl-service.properties w katalogu {INSTALL_DIR}. Plik musi zawierać następujące parametry:
    * service.host - adres IP tej maszyny
    * service.port - port na którym nasłuchuje serwis

    * console.host - adres IP maszyny na której zainstalowana jest konsola
    * console.port - port na którym nasłuchuje konsola

    * db.url - adres bazy danych
    * db.username - użytkownik bazy danych
    * db.password - hasło podanego użytkownika do bazy danych

   Przykładowy plik konfiguracyjny:
   -------------------------------
   service.host=192.168.0.19
   service.port=30303

   console.host=89.65.26.46
   console.port=30304

   db.url=jdbc:postgresql://localhost:5432/db
   db.username=rmartyna
   db.password=rmartyna
   -------------------------------

7. Uruchomić polecenie w katalogu z plikiem run-service.sh
    * chmod +x run-service.sh

8. Edytować zawartość pliku run-service.sh i ustawić zmienną INSTALL_DIR, np.
    * INSTALL_DIR=/home/rmartyna/Projects/asl-service

9. Uruchomić program poleceniem ./run-service.sh

10. Logi z działania programu dostępne są w pliku {INSTALL_DIR}/logs/service.log