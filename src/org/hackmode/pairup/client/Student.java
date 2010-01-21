package org.hackmode.pairup.client;

/**
 * Oh geez I'm making a class Student.
 */
public class Student {
  private String username;
  private String name;

  public Student(String username, String name) {
    this.username = username;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getUsername() {
    return username;
  }
}
