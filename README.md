NutriFit 2.3

Szkolna aplikacja na Androida napisana w Java, XML i Material 3. Konto i dane pozostają na urządzeniu. Uruchamianie serwera nie jest wymagane.

Funkcje

120 przepisów w 12 kategoriach i 100 produktów. Składniki, kroki przygotowania, liczba porcji, orientacyjna kaloryczność oraz Białko/Tłuszcze/Węglowodany (B/T/W).

Wyszukiwanie według nazwy i składników, kategorie, filtry żywieniowe i alergenów, ulubione.

Dziennik z podziałem na daty, masa ciała i wykres, kontrola spożycia wody, profil i zalecenia.

Lokalna rejestracja, logowanie, zmiana hasła, odzyskiwanie za pomocą zapisanego kodu.

Demo NutriFit Plus: plan żywieniowy według dat, przenoszenie zjedzonych posiłków do dziennika, automatyczna lista zakupów, dodatkowe lekcje. Funkcja jest włączana bezpłatnie i nie powoduje żadnych opłat.

6 wbudowanych lekcji tekstowych i 8 linków do materiałów wideo autorów. Można dołączyć własny film za pomocą systemowego wyboru pliku i odtwarzać go wewnątrz aplikacji.

Jasna paleta kolorów, zielone akcenty, kolorowe karty katalogu, fotograficzna okładka, pięć sekcji nawigacji.

Języki

Przełącznik „Język / Language” jest dostępny na ekranie logowania oraz w profilu. Interfejs jest dostępny w języku rosyjskim, angielskim i polskim; wybór jest zapisywany za pomocą mechanizmów Android/AppCompat. Zmieniane są również formaty dat oraz nazwy posiłków. Przepisy, produkty, składniki, instrukcje i wbudowane lekcje są w pełni przetłumaczone. API Google ML Kit tłumaczy treści na telefonie. Po wybraniu języka angielskiego lub polskiego naciśnij „Przygotuj tłumaczenie”. Modele pobierane są przez Wi-Fi, a wyniki zapisywane w SQLite. Po pobraniu modeli tłumaczenie działa offline. Przed pobraniem można korzystać z wbudowanych tłumaczeń w bazie. Tłumaczenie maszynowe może być niedokładne. Ścieżka dźwiękowa zewnętrznych filmów pozostaje w języku autora. Tłumaczenia interfejsu znajdują się w res/values-en/strings.xml i res/values-pl/strings.xml.

Uruchomienie

Otwórz folder zawierający settings.gradle w Android Studio.

Zainstaluj SDK Platform 36, wybierz JDK 17+ (sprawdzone na JBR 21), a następnie wykonaj Gradle Sync.

Uruchom moduł app, wariant debug, na Androidzie 8.0 lub nowszym.

Zarejestruj się, zapisz wyświetlony kod odzyskiwania i uzupełnij demonstracyjny profil.

Otwórz „Plan” i włącz demo Plus, aby zapoznać się z dodatkowymi funkcjami.

Gotowe pliki: delivery/NutriFit-2.3-debug.apk oraz delivery/NutriFit-AndroidStudio.zip. APK jest podpisany kluczem debugowym i przeznaczony do demonstracji szkolnej. ZIP nie zawiera pamięci podręcznych kompilacji ani lokalnych ścieżek do SDK.

Struktura

Trzy główne foldery: app — aplikacja, gradle — system budowania, project — dokumentacja i narzędzia pomocnicze.

app/src/main/java/com/nutrifit/app/
  ui/       ekrany i okna dialogowe
  data/     SQLite, konto, profil, repozytorium
  model/    modele danych
  domain/   obliczenia, filtry, reguły konta
app/src/main/assets/       catalog.db — gotowa baza SQLite
app/src/main/res/layout/   ekrany XML i karty
app/src/main/res/values/   ciągi tekstowe, listy, motyw i kolory
app/src/test/              testy jednostkowe
app/src/androidTest/       testy SQLite i konta na urządzeniu
gradle/wrapper/            uruchamianie wymaganej wersji Gradle
project/                   dokumentacja, skrypty, źródła tłumaczeń
project/screenshots/       zrzuty ekranu interfejsu do dokumentacji


W folderze project przeznaczenie plików wynika z ich nazw:

*.md — architektura, źródła, testy i scenariusz prezentacji.

catalog_schema.sql — schemat katalogu; check_database.py — kontrola integralności, relacji i zawartości SQLite.

check_translations.py, emulator_check.py — skrypty kontrolne.

format_resources.py — formatowanie XML; package_project.py — pakowanie gotowej wersji i kodu źródłowego.

W katalogu głównym znajdują się README.md, ustawienia Gradle oraz skrypty uruchamiania gradlew / gradlew.bat. Folder delivery/ jest przeznaczony na gotowe pliki APK i ZIP. Foldery build/, app/build/, .gradle/, .idea/ są tworzone przez narzędzia i wykluczone z Git, podobnie jak lokalny plik local.properties. Nie trzeba ich usuwać w celu uproszczenia kodu źródłowego: przy następnym budowaniu zostaną utworzone ponownie.

Struktura app/src jest standardowa dla Android Studio. W kodzie Java zachowano cztery sekcje: ekrany, dane, modele i logika. W Android Studio wybierz widok Android — pokazuje on pakiety i zasoby w kompaktowym drzewie.

Teksty interfejsu znajdują się w strings.xml, a listy wyboru w zasobach XML. Przepisy, produkty, składniki, kroki, lekcje i tłumaczenia są przechowywane w app/src/main/assets/catalog.db. Nie ma plików JSON ani pól JSON w bazie. Instrukcja edycji katalogu i migracji: [DATABASE.md](project/DATABASE.md).

Budowanie i testy
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat assembleQa connectedQaAndroidTest
python project/check_database.py
python project/check_translations.py
python project/package_project.py


Drugie polecenie wymaga podłączonego emulatora. Wariant qa używa osobnego pakietu com.nutrifit.app.qa. Testy dodatkowo izolują swoje bazy danych i ustawienia za pomocą losowych nazw.

Działanie bez internetu

SQLite przechowuje katalog, ulubione, dziennik, plan, zakupy, wodę, masę ciała oraz postęp w lekcjach. Prywatne SharedPreferences przechowują profil, dostęp demonstracyjny Plus, sól oraz hash hasła/kodu odzyskiwania. Hasło i kod nie są przechowywane w postaci jawnej. Haszowanie oraz operacje na bazie danych wykonywane są poza głównym wątkiem.

Internet służy ML Kit do pobierania modeli językowych. Teksty tłumaczone są na urządzeniu; konto, dziennik i katalog nie wymagają serwera. SDK przesyła Google techniczne metryki działania, ale nie wysyła tekstów do tłumaczenia w chmurze. Zewnętrzne filmy są otwierane po kliknięciu w przeglądarce lub aplikacji platformy wideo i wymagają dostępu do internetu. Wbudowane lekcje i katalog są dostępne offline. Lokalne wideo jest dostępne, dopóki wybrany plik istnieje, a system udostępnia do niego dostęp.

Jedno konto osobiste oraz osobny profil demo bez hasła na instalację. Demo używa oddzielnych plików bazy i ustawień. Usunięcie aplikacji usuwa jej dane; kopie zapasowe w chmurze oraz przenoszenie danych są wyłączone. Istniejące konta mogą utworzyć kod odzyskiwania w profilu po wprowadzeniu aktualnego hasła.

B/T/W, masa gotowego dania oraz czas przygotowania są wartościami demonstracyjnymi zależnymi od produktów i sposobu przygotowania. Filtr uwzględnia wskazane składniki; skład opakowania oraz możliwe śladowe ilości alergenów należy sprawdzać osobno. Obliczenia profilu są przeznaczone dla demonstracyjnego scenariusza dla osoby dorosłej, a nie do celów medycznych.

Szczegóły: [architektura](project/IMPLEMENTATION.md), [źródła](project/SOURCES.md), [testy](project/TESTING.md), [scenariusz prezentacji](project/PROJECT.md).

Szczegóły dotyczące tłumaczeń, optymalizacji i animacji: [LOCALIZATION.md](project/LOCALIZATION.md).
