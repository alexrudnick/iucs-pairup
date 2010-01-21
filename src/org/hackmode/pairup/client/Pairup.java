package org.hackmode.pairup.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Pairup implements EntryPoint {
  private ListBox rosterbox;

  private HashMap<String, Section> sections = new HashMap<String, Section>();
  private HashMap<String, CheckBox> studentCheckboxes = new HashMap<String, CheckBox>();

  private String currentSection = "";

  private FlexTable studentTable;
  private VerticalPanel teamsPanel;

  // If this is set, we can send this right back up, when it's time to save. It
  // contains the section number and all of the teams.
  private String teamsJson = null;
  private int nStudentsInTable = 0;

  public void onModuleLoad() {
    buildUI();
    editMode();

    // grab roster data from server, parse it, and put it in the UI.
    downloadRosters();
  }

  protected void addGuest() {
    if ("".equals(currentSection)) {
      Window.alert("Pick a section!");
      return;
    }
    String username = null;
    String name = null;

    username = Window.prompt("Guest username?", "");

    if (null != username) {
      name = Window.prompt("Guest full name?", "");
    }

    if (username != null && name != null && !"".equals(username)
        && !"".equals(name)) {
      Section sec = sections.get(currentSection);
      Student guest = new Student(username, name);
      sec.addStudent(guest);
      addStudentCheckbox(guest);
    }
  }

  protected void rememberTeams(String text) {
    this.teamsJson = text;
  }

  /**
   * Create a new checkbox for a student's username. Should only be called once
   * the studentListPanel has already been initialized.
   */
  private void addStudentCheckbox(Student student) {
    if (studentTable == null || !studentTable.isAttached()) {
      Window.alert("addStudentCheckbox shouldn't have been called yet.");
      return;
    }

    CheckBox cb = new CheckBox(student.getUsername());
    cb.setValue(true);
    studentCheckboxes.put(student.getUsername(), cb);

    // studentTable.add(cb);
    int row = nStudentsInTable;
    studentTable.setWidget(row, 0, cb);
    studentTable.setWidget(row, 1, new Label(student.getName()));

    nStudentsInTable++;
  }

  private void buildUI() {
    // Controls area at the top
    RootPanel controls = RootPanel.get("controls");
    FlowPanel panel = new FlowPanel();
    panel.setStyleName("controlpanel");

    controls.add(panel);

    rosterbox = new ListBox(false);
    Button pickbutton = new Button("Pick!");
    Button makeTeams = new Button("Make Teams!");
    Button addGuest = new Button("Add Guest");

    rosterbox.addItem("Pick a roster", "");

    panel.add(rosterbox);
    panel.add(pickbutton);
    panel.add(addGuest);
    panel.add(makeTeams);

    pickbutton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        currentSection = rosterbox.getItemText(rosterbox.getSelectedIndex());
        setMessage(currentSection);
        populateStudentList();
      }
    });

    makeTeams.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        getTeams();
      }
    });

    addGuest.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        addGuest();
      }
    });

    // List of students area
    studentTable = new FlexTable();
    RootPanel.get("studentlist").add(studentTable);

    // List of teams area
    teamsPanel = new VerticalPanel();
    RootPanel.get("teams").add(teamsPanel);

    // Bottom area, for when you're almost done.
    Button saveteams = new Button("Save Teams!");
    Button editAgain = new Button("Edit Again!");
    FlowPanel bottomPanel = new FlowPanel();

    bottomPanel.add(saveteams);
    bottomPanel.add(editAgain);
    RootPanel.get("bottomarea").add(bottomPanel);

    saveteams.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        saveTeams();
        doneMode();
      }
    });

    editAgain.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        editMode();
      }
    });
  }

  /**
   * Takes the json text returned by the server, parses it, and shows the
   * resulting teams in the UI. The protocol is: the first thing in the list is
   * the section number, and everything following is the individual teams.
   */
  private void displayTeams(String text) {
    JSONValue parsed = JSONParser.parse(text);
    JSONArray jteams = parsed.isArray();
    Section section = sections.get(currentSection);

    teamsPanel.clear();

    for (int i = 1; i < jteams.size(); i++) {
      JSONArray jteam = jteams.get(i).isArray();

      int teamsize = jteam.size();
      String team = "";

      for (int member = 0; member < teamsize; member++) {
        String username = jteam.get(member).isString().stringValue();
        String name = section.getStudentName(username);

        if (member > 0) {
          team += ", ";
        }

        team += name;
      }
      teamsPanel.add(new Label(team));
    }
  }

  private void doneMode() {
    RootPanel.get("toparea").setVisible(false);
    RootPanel.get("teams").setVisible(true);
    RootPanel.get("bottomarea").setVisible(false);
  }

  /**
   * Download the json data for all the rosters from the server.
   */
  private void downloadRosters() {
    RequestBuilder req = new RequestBuilder(RequestBuilder.GET,
        GWT.getHostPageBaseURL() + "rosters.cgi");
    setMessage("Pick a section.");

    req.setCallback(new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        setMessage("hrm, ajax error, in onError: " + exception.getMessage());
      }

      public void onResponseReceived(Request request, Response response) {
        setRosters(response.getText());
        showRosters();
      }
    });

    try {
      req.send();
    } catch (RequestException e) {
      setMessage("hrm, ajax error: " + e.getMessage());
    }
  }

  private void editMode() {
    RootPanel.get("toparea").setVisible(true);
    RootPanel.get("teams").setVisible(false);
    RootPanel.get("bottomarea").setVisible(false);
  }

  /**
   * Go to the server and get the teams for today. Also update the UI.
   * 
   * The protocol for this is that the first thing in the list we send is the
   * current section number, and everything else is student usernames.
   * 
   * @throws RequestException
   */
  private void getTeams() {
    JSONArray toSend = new JSONArray();

    if ("".equals(currentSection)) {
      Window.alert("Pick a section!");
      return;
    }

    toSend.set(0, new JSONString(currentSection));
    int i = 1;
    Section section = sections.get(currentSection);
    toSend.set(i, new JSONString(section.getName()));

    for (String username : sections.get(currentSection).getStudentMap().keySet()) {
      CheckBox cb = studentCheckboxes.get(username);

      if (cb.getValue()) {
        toSend.set(i, new JSONString(username));
        i++;
      } else {

      }
    }

    RequestBuilder req = new RequestBuilder(RequestBuilder.POST,
        GWT.getHostPageBaseURL() + "getteams.cgi");
    req.setRequestData(toSend.toString());

    req.setCallback(new RequestCallback() {
      public void onError(Request request, Throwable exception) {
        setMessage("oh noes.");
      }

      public void onResponseReceived(Request request, Response response) {
        setMessage("hooray!");
        rememberTeams(response.getText());
        displayTeams(response.getText());
        saveMode();
      }
    });

    try {
      req.send();
    } catch (RequestException e) {
      Window.alert("ajax fail.");
    }
  }

  /**
   * Build the UI that shows all of the students in the current section, by
   * username, with associated checkboxes.
   */
  private void populateStudentList() {
    HashMap<String, Student> sectionRoster = sections.get(currentSection).getStudentMap();

    studentTable.clear();
    nStudentsInTable = 0;

    ArrayList<String> usernames = new ArrayList<String>();
    for (String username : sectionRoster.keySet()) {
      usernames.add(username);
    }
    Collections.sort(usernames);

    for (String username : usernames) {
      addStudentCheckbox(sectionRoster.get(username));
    }
  }

  private void saveMode() {
    RootPanel.get("toparea").setVisible(false);
    RootPanel.get("teams").setVisible(true);
    RootPanel.get("bottomarea").setVisible(true);
  }

  private void saveTeams() {
    if (teamsJson == null) {
      Window.alert("no teams data to save!");
      return;
    }

    RequestBuilder req = new RequestBuilder(RequestBuilder.POST,
        GWT.getHostPageBaseURL() + "saveteams.cgi");
    req.setRequestData(teamsJson);
    req.setCallback(new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        setMessage("Error saving team data.");
      }

      public void onResponseReceived(Request request, Response response) {
        setMessage(response.getStatusCode() + "");
      }
    });
    try {
      req.send();
    } catch (RequestException e) {
      setMessage("Error saving team data.");
    }
  }

  private void setMessage(String text) {
    Element elt = RootPanel.get("message").getElement();
    elt.setInnerText(text);
  }

  private void setRosters(String text) {
    JSONValue parsedRosters = JSONParser.parse(text);

    for (String secname : parsedRosters.isObject().keySet()) {
      sections.put(secname, new Section(secname, parsedRosters.isObject().get(
          secname).isObject()));
    }
  }

  private void showRosters() {
    rosterbox.clear();

    for (String section : sections.keySet()) {
      rosterbox.addItem(section);
    }
  }
}