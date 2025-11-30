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
        List<String> testWords = Arrays.asList(
            "стол", "стул", "окно", "дверь", "книга",
            "ручка", "бумага", "компьютер", "мышь", "клава"
        );
        dictionary = new WordleDictionary(testWords);
        loader = new WordleDictionaryLoader();
    }

    @Test
    void testDictionaryNormalization() throws IOException {
        // Создаем временный файл словаря
        File dictFile = tempDir.resolve("test_dict.txt").toFile();
        try (PrintWriter writer = new PrintWriter(dictFile, StandardCharsets.UTF_8)) {
            writer.println("СтОл");
            writer.println("СТУЛ");
            writer.println("окно");
            writer.println("дверь");
            writer.println("ёлка"); // тест замены ё на е
            writer.println("мёд");
        }

        WordleDictionary loadedDict = loader.loadDictionary(dictFile.getAbsolutePath());
        List<String> words = loadedDict.getWords();

        assertTrue(words.contains("стол"));
        assertTrue(words.contains("стул"));
        assertTrue(words.contains("окно"));
        assertTrue(words.contains("дверь"));
        assertTrue(words.contains("елка")); // после нормализации
        assertTrue(words.contains("мед"));  // после нормализации
    }

    @Test
    void testDictionaryContains() {
        assertTrue(dictionary.contains("стол"));
        assertTrue(dictionary.contains("СТОЛ")); // регистр не важен
        assertTrue(dictionary.contains("СтОл")); // смешанный регистр
        assertFalse(dictionary.contains("несуществующее"));
    }

    @Test
    void testWordAnalysis() {
        // Правильные позиции
        assertEquals("+++++", WordleDictionary.analyzeWord("стол", "стол"));
        assertEquals("+---", WordleDictionary.analyzeWord("стул", "стол").substring(0, 4));
        assertEquals("+^--", WordleDictionary.analyzeWord("слот", "стол").substring(0, 4));

        // Буквы в неправильных позициях
        assertEquals("-^--", WordleDictionary.analyzeWord("луна", "стул").substring(0, 4));
        assertEquals("^^--", WordleDictionary.analyzeWord("толс", "стол").substring(0, 4));
    }

    @Test
    void testGameInitialization() {
        WordleGame game = new WordleGame(dictionary);

        assertEquals(6, game.getRemainingSteps());
        assertFalse(game.isGameOver());
        assertFalse(game.isWon());
        assertNotNull(game.getAnswer());
        assertTrue(game.getAttempts().isEmpty());
    }

    @Test
    void testSuccessfulAttempt() throws WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        String answer = game.getAnswer();

        WordleGame.GameResult result = game.makeAttempt(answer);

        assertTrue(result.isWin());
        assertEquals("+++++", result.getAnalysis());
        assertTrue(game.isWon());
        assertTrue(game.isGameOver());
    }

    @Test
    void testFailedAttempt() throws WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        String answer = game.getAnswer();
        String wrongWord = "стул"; // гарантированно другое слово

        if (!wrongWord.equals(answer)) {
            WordleGame.GameResult result = game.makeAttempt(wrongWord);

            assertFalse(result.isWin());
            assertNotNull(result.getAnalysis());
            assertEquals(5, game.getRemainingSteps());
            assertFalse(game.isWon());
            assertFalse(game.isGameOver());
        }
    }

    @Test
    void testInvalidWordLength() {
        WordleGame game = new WordleGame(dictionary);

        assertThrows(WordleGameException.class, () -> game.makeAttempt("короткое"));
        assertThrows(WordleGameException.class, () -> game.makeAttempt("дл"));
    }

    @Test
    void testWordNotInDictionary() {
        WordleGame game = new WordleGame(dictionary);

        assertThrows(WordNotFoundInDictionaryException.class,
            () -> game.makeAttempt("абвгд")); // несуществующее слово
    }

    @Test
    void testGameOverAfterSixAttempts() throws WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        String wrongWord = "стул";

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
        assertThrows(WordleGameException.class, () -> game.makeAttempt("стол"));
    }

    @Test
    void testHintSystem() throws WordleGameException {
        WordleGame game = new WordleGame(dictionary);

        String hint = game.getHint();
        assertNotNull(hint);
        assertTrue(dictionary.contains(hint));

        // После ввода слова подсказка должна учитывать новые данные
        game.makeAttempt("стул");
        String newHint = game.getHint();
        assertNotNull(newHint);
    }

    @Test
    void testLetterTracking() throws WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        String answer = game.getAnswer();

        // Делаем попытку и проверяем, что информация о буквах обновляется
        game.makeAttempt("стул");

        // Проверяем, что попытка записана
        assertEquals(1, game.getAttempts().size());
        assertEquals("стул", game.getAttempts().get(0));
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
            writer.println("ёлка");
            writer.println("мёд");
            writer.println("берёза");
        }

        WordleDictionary loadedDict = loader.loadDictionary(dictFile.getAbsolutePath());

        assertTrue(loadedDict.contains("елка"));
        assertTrue(loadedDict.contains("мед"));
        assertTrue(loadedDict.contains("береза"));
    }

    @Test
    void testRandomWordSelection() {
        // Проверяем, что случайные слова действительно из словаря
        for (int i = 0; i < 10; i++) {
            String randomWord = dictionary.getRandomWord();
            assertTrue(dictionary.contains(randomWord));
        }
    }

    @Test
    void testGameResult() {
        WordleGame.GameResult result = new WordleGame.GameResult(true, "+++++", "test");

        assertTrue(result.isWin());
        assertEquals("+++++", result.getAnalysis());
        assertEquals("test", result.getWord());
    }
}
