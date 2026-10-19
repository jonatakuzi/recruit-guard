package com.jonatakuzi.recruitguard.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Small, pure helper functions for working with domain names.
 * No Spring, no state - which makes every method easy to unit test.
 */
public final class DomainUtils {

    /** Suffixes where the registrable domain is three labels, e.g. example.co.uk. */
    private static final Set<String> TWO_PART_SUFFIXES = Set.of(
            "co.uk", "org.uk", "ac.uk", "com.au", "co.in", "co.jp", "com.br");

    /** Characters scammers swap in because they look alike at a glance. */
    private static final Map<Character, Character> HOMOGLYPHS = Map.of(
            '0', 'o',
            '1', 'l',
            '3', 'e',
            '5', 's',
            '7', 't',
            '4', 'a');

    private DomainUtils() {
    }

    /** Pulls the domain out of an email address: "Jane <jane@acme.com>" -> "acme.com". */
    public static String domainFromEmail(String email) {
        if (email == null) {
            return "";
        }
        String cleaned = email.trim();
        int lt = cleaned.lastIndexOf('<');
        int gt = cleaned.lastIndexOf('>');
        if (lt >= 0 && gt > lt) {
            cleaned = cleaned.substring(lt + 1, gt);
        }
        int at = cleaned.lastIndexOf('@');
        String domain = at >= 0 ? cleaned.substring(at + 1) : cleaned;
        return domain.trim().toLowerCase(Locale.ROOT).replaceAll("\\.$", "");
    }

    /**
     * Returns the part of a domain that someone actually registers.
     * "careers.ocient.com" -> "ocient.com"; "jobs.example.co.uk" -> "example.co.uk".
     * This matters because "ocient.com.hr-portal.net" is really owned by hr-portal.net.
     */
    public static String registrableDomain(String domain) {
        String[] labels = domain.toLowerCase(Locale.ROOT).split("\\.");
        if (labels.length <= 2) {
            return domain.toLowerCase(Locale.ROOT);
        }
        String lastTwo = labels[labels.length - 2] + "." + labels[labels.length - 1];
        int keep = TWO_PART_SUFFIXES.contains(lastTwo) ? 3 : 2;
        StringBuilder sb = new StringBuilder();
        for (int i = labels.length - keep; i < labels.length; i++) {
            if (!sb.isEmpty()) {
                sb.append('.');
            }
            sb.append(labels[i]);
        }
        return sb.toString();
    }

    /** The name part of a registrable domain without its suffix: "ocient.com" -> "ocient". */
    public static String nameWithoutSuffix(String registrable) {
        int dot = registrable.indexOf('.');
        return dot >= 0 ? registrable.substring(0, dot) : registrable;
    }

    /** The suffix of a registrable domain: "ocient.com" -> "com". */
    public static String suffix(String registrable) {
        int dot = registrable.indexOf('.');
        return dot >= 0 ? registrable.substring(dot + 1) : "";
    }

    /**
     * Undoes common look-alike tricks so "0cient" and "rnicrosoft" compare equal
     * to "ocient" and "microsoft". Hyphens are dropped too.
     */
    public static String normalizeLookalikes(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '-') {
                continue;
            }
            sb.append(HOMOGLYPHS.getOrDefault(c, c));
        }
        return sb.toString().replace("rn", "m").replace("vv", "w");
    }

    /**
     * Levenshtein edit distance: the minimum number of single-character
     * inserts, deletes, or substitutions to turn {@code a} into {@code b}.
     * Classic dynamic-programming solution using two rows, O(n*m) time, O(m) space.
     */
    public static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
