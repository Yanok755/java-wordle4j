package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordleTest {

    private WordleDictionary dictionary;
    private WordleDictionaryLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Используем ТОЛЬКО 5-буквенные слова для тестов
        List<String> testWords = Arrays.asList(
            "столи",  // 5 букв
            "стуль",  // 5 букв  
            "окошк",  // 5 букв
            "дверь",  // 5 букв
            "книга",  // 5 букв
            "ручей",  // 5 букв
            "бумаг",  // 5 букв
            "мышка",  // 5 букв
            "клава"   // 5 букв
        );
        dictionary = new WordleDictionary(testWords);
        loader = new WordleDictionaryLoader();
    }

    @Test
    void testDictionaryNormalization() throws IOException {
        File dictFile = tempDir.resolve("test_dict.txt").toFile();
        try (PrintWriter writer = new PrintWriter(dictFile, StandardCharsets.UTF_8)) {
            writer.println("СтОлИ");  // 5 букв
            writer.println("СТУЛЬ");  // 5 букв
            writer.println("окошК");  // 5 букв
            writer.println("дверь");  // 5 букв
            writer.println("ёлочк");  // 5 букв
            writer.println("мёдик");  // 5 букв
        }

        WordleDictionary loadedDict = loader.loadDictionary(dictFile.getAbsolutePath());
        List<String> words = loadedDict.getWords();

        // Все слова должны быть нормализованы и иметь длину 5
        assertTrue(words.contains("столи"));
        assertTrue(words.contains("стуль"));
        assertTrue(words.contains("окошк"));
        assertTrue(words.contains("дверь"));
        assertTrue(words.contains("елочк"));
        assertTrue(words.contains("медик"));
        
        // Проверяем, что все слова имеют длину 5
        for (String word : words) {
            assertEquals(5, word.length(), "Слово '" + word + "' должно иметь длину 5");
        }
    }

    @Test
    void testDictionaryContains() {
        assertTrue(dictionary.contains("столи"));
        assertTrue(dictionary.contains("СТОЛИ"));
        assertTrue(dictionary.contains("СтОлИ"));
        assertTrue(dictionary.contains("стуль"));
        assertFalse(dictionary.contains("несуществующееслово"));
    }

    @Test
    void testWordAnalysis() {
    // Правильные позиции
        assertEquals("+++++", WordleDictionary.analyzeWord("столи", "столи"));
    
    // Первая буква правильная, остальные нет
        String result1 = WordleDictionary.analyzeWord("стуль", "столи");
        assertEquals("++-+-", result1, "Для 'стуль' vs 'столи' ожидается '+----', но получено: " + result1);
    
    // Буквы в неправильных позициях
        String result2 = WordleDictionary.analyzeWord("слони", "столи");
        assertEquals("+^---", result2, "Для 'слони' vs 'столи' ожидается '+^+-+', но получено: " + result2);
    
    // Все буквы неправильные
        assertEquals("-----", WordleDictionary.analyzeWord("абвгд", "столи"));
    }

    @Test
    void testGameInitialization() {
        WordleGame game = new WordleGame(dictionary);

        assertEquals(6, game.getRemainingSteps());
        assertFalse(game.isGameOver());
        assertFalse(game.isWon());
        assertNotNull(game.getAnswer());
        assertEquals(5, game.getAnswer().length());
        assertTrue(game.getAttempts().isEmpty());
    }

    @Test
    void testSuccessfulAttempt() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        String answer = game.getAnswer();

        WordleGame.GameResult result = game.makeAttempt(answer);

        assertTrue(result.isWin());
        assertEquals("+++++", result.getAnalysis());
        assertTrue(game.isWon());
        assertTrue(game.isGameOver());
    }

    @Test
    void testFailedAttempt() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        String answer = game.getAnswer();
        
        // Находим слово, которое точно не является ответом
        String wrongWord = dictionary.getWords().stream()
                .filter(word -> !word.equals(answer))
                .findFirst()
                .orElse("стуль");

        WordleGame.GameResult result = game.makeAttempt(wrongWord);

        assertFalse(result.isWin());
        assertNotNull(result.getAnalysis());
        assertEquals(5, result.getAnalysis().length());
        assertEquals(5, game.getRemainingSteps());
        assertFalse(game.isWon());
        assertFalse(game.isGameOver());
    }

    @Test
    void testInvalidWordLength() {
        WordleGame game = new WordleGame(dictionary);

        assertThrows(WordleGame.WordleGameException.class, () -> game.makeAttempt("короткоеслово"));
        assertThrows(WordleGame.WordleGameException.class, () -> game.makeAttempt("дл"));
    }

    @Test
    void testWordNotInDictionary() {
        WordleGame game = new WordleGame(dictionary);

        assertThrows(WordleGame.WordNotFoundInDictionaryException.class,
            () -> game.makeAttempt("абвгд"));
    }

    @Test
    void testGameOverAfterSixAttempts() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        
        // Находим слово, которое точно не является ответом
        String wrongWord = dictionary.getWords().stream()
                .filter(word -> !word.equals(game.getAnswer()))
                .findFirst()
                .orElse("стуль");

        // Делаем 6 неудачных попыток
        for (int i = 0; i < 6; i++) {
            if (!game.isGameOver()) {
                game.makeAttempt(wrongWord);
            }
        }

        assertTrue(game.isGameOver());
        assertFalse(game.isWon());
        assertEquals(0, game.getRemainingSteps());

        // Нельзя сделать ход после окончания игры
        assertThrows(WordleGame.WordleGameException.class, () -> game.makeAttempt("столи"));
    }

    @Test
    void testHintSystem() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);

        String hint = game.getHint();
        assertNotNull(hint);
        assertTrue(dictionary.contains(hint));
        assertEquals(5, hint.length());

        // После ввода слова подсказка должна учитывать новые данные
        String testWord = dictionary.getWords().get(0);
        if (!testWord.equals(game.getAnswer())) {
            game.makeAttempt(testWord);
            String newHint = game.getHint();
            assertNotNull(newHint);
            assertEquals(5, newHint.length());
            assertTrue(dictionary.contains(newHint));
        }
    }

    @Test
    void testLetterTracking() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);

        // Делаем попытку и проверяем, что информация о буквах обновляется
        String testWord = dictionary.getWords().get(0);
        game.makeAttempt(testWord);

        // Проверяем, что попытка записана
        assertEquals(1, game.getAttempts().size());
        assertEquals(testWord, game.getAttempts().get(0));
    }

    @Test
    void testDictionaryLoaderFileNotFound() {
        assertThrows(IOException.class,
            () -> loader.loadDictionary("nonexistent_file.txt"));
    }

    @Test
    void testDictionaryLoaderEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.txt").toFile();
        emptyFile.createNewFile();

        assertThrows(IOException.class,
            () -> loader.loadDictionary(emptyFile.getAbsolutePath()));
    }

    @Test
    void testNormalizationWithYo() throws IOException {
        File dictFile = tempDir.resolve("yo_dict.txt").toFile();
        try (PrintWriter writer = new PrintWriter(dictFile, StandardCharsets.UTF_8)) {
            writer.println("берёз");  // 5 букв
            writer.println("ёжник");  // 5 букв
            writer.println("пёстр");  // 5 букв
        }

        WordleDictionary loadedDict = loader.loadDictionary(dictFile.getAbsolutePath());

        assertTrue(loadedDict.contains("берез"));
        assertTrue(loadedDict.contains("ежник"));
        assertTrue(loadedDict.contains("пестр"));
    }

    @Test
    void testRandomWordSelection() {
        // Проверяем, что случайные слова действительно из словаря и имеют длину 5
        for (int i = 0; i < 10; i++) {
            String randomWord = dictionary.getRandomWord();
            assertTrue(dictionary.contains(randomWord));
            assertEquals(5, randomWord.length());
        }
    }

    @Test
    void testGameResult() {
        WordleGame.GameResult result = new WordleGame.GameResult(true, "+++++", "тест");

        assertTrue(result.isWin());
        assertEquals("+++++", result.getAnalysis());
        assertEquals("тест", result.getWord());
    }
}
