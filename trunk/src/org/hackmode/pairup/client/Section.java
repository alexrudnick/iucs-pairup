package org.hackmode.pairup.client;

import com.google.gwt.json.client.JSONObject;

import java.util.HashMap;

/**
 * Our representation in memory (on the client side) of a section. It knows its
 * name and which students are in it. It also knows how to add guest students.
 */
public class Section {
  private String name;
  private HashMap<String, Student> students;

  public Section(String name, JSONObject jsonStudents) {
    this.name = name;
    students = new HashMap<String, Student>();
    
    for (String username : jsonStudents.keySet()) {
      students.put(username, new Student(username,
          jsonStudents.get(username).isString().stringValue()));
    }
  }

  public String getName() {
    return name;
  }
  
  public String getStudentName(String username) {
    return students.get(username).getName();
  }

  public HashMap<String, Student> getStudentMap() {
    return students;
  }

  public void addStudent(Student student) {
    students.put(student.getUsername(), student);    
  }
}
