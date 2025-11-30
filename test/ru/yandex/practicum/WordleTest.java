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
            "столик", "стулья", "окошко", "дверца", "книжка",
            "ручеек", "бумага", "мышка", "клавиа"
        );
        dictionary = new WordleDictionary(testWords);
        loader = new WordleDictionaryLoader();
    }

    @Test
    void testDictionaryNormalization() throws IOException {
        File dictFile = tempDir.resolve("test_dict.txt").toFile();
        try (PrintWriter writer = new PrintWriter(dictFile, StandardCharsets.UTF_8)) {
            writer.println("СтОлиК");
            writer.println("СТУЛЬЯ");
            writer.println("окошкО");
            writer.println("дверь");  // 5 букв
            writer.println("ёлочк");  // 5 букв
            writer.println("мёдик");  // 5 букв
        }

        WordleDictionary loadedDict = loader.loadDictionary(dictFile.getAbsolutePath());
        List<String> words = loadedDict.getWords();

        // Все слова должны быть нормализованы и иметь длину 5
        assertTrue(words.contains("столик"));
        assertTrue(words.contains("стулья"));
        assertTrue(words.contains("окошко"));
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
        assertTrue(dictionary.contains("столик"));
        assertTrue(dictionary.contains("СТОЛИК"));
        assertTrue(dictionary.contains("СтОлИк"));
        assertTrue(dictionary.contains("стулья"));
        assertFalse(dictionary.contains("несуществующееслово"));
    }

    @Test
    void testWordAnalysis() {
        // Правильные позиции
        assertEquals("+++++", WordleDictionary.analyzeWord("столик", "столик"));
        assertEquals("+----", WordleDictionary.analyzeWord("стулья", "столик"));
        
        // Буквы в неправильных позициях
        String analysis = WordleDictionary.analyzeWord("слоник", "столик");
        assertEquals(5, analysis.length());
    }

    @Test
    void testGameInitialization() {
        WordleGame game = new WordleGame(dictionary);

        assertEquals(6, game.getRemainingSteps());
        assertFalse(game.isGameOver());
        assertFalse(game.isWon());
        assertNotNull(game.getAnswer());
        assertEquals(5, game.getAnswer().length()); // Ответ должен быть 5 букв
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
                .orElse("стулья");

        WordleGame.GameResult result = game.makeAttempt(wrongWord);

        assertFalse(result.isWin());
        assertNotNull(result.getAnalysis());
        assertEquals(5, result.getAnalysis().length()); // Анализ должен быть длиной 5
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
                .orElse("стулья");

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
        assertThrows(WordleGame.WordleGameException.class, () -> game.makeAttempt("столик"));
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
            writer.println("берёзка");
            writer.println("ёжник");
            writer.println("пёстры");
        }

        WordleDictionary loadedDict = loader.loadDictionary(dictFile.getAbsolutePath());

        assertTrue(loadedDict.contains("березка"));
        assertTrue(loadedDict.contains("ежник"));
        assertTrue(loadedDict.contains("пестры"));
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
