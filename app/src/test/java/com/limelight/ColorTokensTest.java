package com.limelight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The light palette (values/colors.xml) and the dark one (values-night/colors.xml) must define
 * the same colours. A colour missing from the night file would silently show its light value
 * in dark mode, for example a pale panel on a dark screen.
 */
public class ColorTokensTest {
    private static final Pattern COLOR = Pattern.compile("<color name=\"([^\"]+)\">");

    @Test
    public void everyLightColourHasADarkOneAndTheOtherWayRound() throws Exception {
        Set<String> light = names("src/main/res/values/colors.xml");
        Set<String> night = names("src/main/res/values-night/colors.xml");

        // Not themed: a brand colour that is the same in both modes
        light.remove("profileAccent");

        Set<String> onlyLight = new TreeSet<>(light);
        onlyLight.removeAll(night);
        Set<String> onlyNight = new TreeSet<>(night);
        onlyNight.removeAll(light);

        assertTrue("missing from values-night/colors.xml: " + onlyLight, onlyLight.isEmpty());
        assertTrue("missing from values/colors.xml: " + onlyNight, onlyNight.isEmpty());
        assertTrue("expected the Ariane colour roles to be defined", light.contains("ariane_background"));
        assertEquals(light, night);
    }

    private static Set<String> names(String path) throws Exception {
        File file = new File(path);
        String xml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        Set<String> names = new TreeSet<>();
        Matcher m = COLOR.matcher(xml);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }
}
