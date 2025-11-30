package ru.yandex.practicum;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WordleDictionaryLoader {

    public WordleDictionary loadDictionary(String filename) throws IOException {
        List<String> words = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String normalizedWord = normalizeWord(line.trim());
                if (normalizedWord.length() == 5) {
                    words.add(normalizedWord);
                }
            }
        }

        if (words.isEmpty()) {
            throw new IOException("Словарь пуст или не содержит слов из 5 букв");
        }

        return new WordleDictionary(words);
    }

    private String normalizeWord(String word) {
        return word.toLowerCase()
                  .replace('ё', 'е')
                  .replaceAll("[^а-я]", "");
    }
}
