package com.nutrifit.app;

import java.util.HashSet;
import java.util.Set;

public class Profile {
    public String name = "";
    public int age = 25;
    public double height = 175, weight = 75, targetWeight = 70;
    public boolean male = true;
    public int activity = 0, goal = 1; // goal: 0 lose, 1 maintain, 2 gain
    public Set<String> likes = new HashSet<>(), dislikes = new HashSet<>();
    public Set<String> allergies = new HashSet<>(), restrictions = new HashSet<>();
}
