package nl.knaw.huygens.repository.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonValue;

@SuppressWarnings("serial")
public class Datable implements Serializable {

  /** Central European Time */
  static final TimeZone CET = TimeZone.getTimeZone("CET");

  public enum Certainty {
    HIGH, MEDIUM, LOW
  }

  private final String edtf;
  private Certainty certainty;
  private Date fromDate;
  private Date toDate;

  public Datable(String edtf) {
    this.edtf = edtf;
    convertFromEDTF(edtf);
  }

  public String getEDTF() {
    return edtf;
  }

  public boolean isValid() {
    return (fromDate != null) && (toDate != null);
  }

  public boolean isRange() {
    return false;
  }

  public Certainty getCertainty() {
    return certainty;
  }

  public Date getFromDate() {
    return fromDate;
  }

  public Date getToDate() {
    return toDate;
  }

  @Override
  @JsonValue
  public String toString() {
    return edtf;
  }

  /**
   * Returns a calendar instance for the default time zone.
   */
  private Calendar getCalendar() {
    return Calendar.getInstance(CET);
  }

  /**
   * Sets the specified date, taken at noon.
   */
  private void set(Calendar calendar, int year, int month, int date) {
    calendar.set(year, month, date, 12, 0);
  }

  private void convertFromEDTF(String text) {
    text = text.replace("<", "").replace(">", "").trim();
    EDTFPattern edtf = EDTFPattern.matchingPattern(text);
    if (edtf == null || edtf.equals("")) {
      setCertainty(Certainty.LOW);
    } else {
      Calendar calendar = getCalendar();
      switch (edtf) {
      case YEAR:
        setCertainty(Certainty.HIGH);
        setFromYear(text);
        break;
      case YEAR_Q:
        setCertainty(Certainty.LOW);
        setFromYear(text.replace("?", ""));
        break;
      case YEAR_A:
        setCertainty(Certainty.MEDIUM);
        setFromYear(text.replace("~", ""));
        break;
      case YEAR_RANGE_Q1:
      case YEAR_RANGE_Q2:
      case YEAR_RANGE_Q3:
        setCertainty(Certainty.LOW);
        int firstYear = Integer.parseInt(text.replace("?", "0"));
        int lastYear = Integer.parseInt(text.replace("?", "9"));
        calendar.clear();
        set(calendar, firstYear, Calendar.JANUARY, 1);
        setFromDate(calendar);
        calendar.clear();
        set(calendar, lastYear, Calendar.DECEMBER, 31);
        setToDate(calendar);
        break;
      case YEAR_MONTH:
        setCertainty(Certainty.HIGH);
        setFromMonthYear(text);
        break;
      case YEAR_MONTH_Q:
        setCertainty(Certainty.LOW);
        setFromMonthYear(text.replace("?", ""));
        break;
      case YEAR_MONTH_A:
        setCertainty(Certainty.MEDIUM);
        setFromMonthYear(text.replace("~", ""));
        break;
      case YEAR_MONTH_RANGE:
        setCertainty(Certainty.MEDIUM);
        setFromYear(text.replace("-??", ""));
        break;
      case DAY_MONTH_YEAR:
        setCertainty(Certainty.HIGH);
        String[] dmy = text.split("-");
        calendar.clear();
        set(calendar, Integer.parseInt(dmy[2]), Integer.parseInt(dmy[1]) - 1, Integer.parseInt(dmy[0]));
        setFromDate(calendar);
        setToDate(calendar);
        break;
      case MONTH_YEAR_RX:
        setCertainty(Certainty.MEDIUM);
        String[] dmy1 = text.split("-");
        setFromMonthYear(dmy1[1] + "-" + dmy1[0]);
        break;
      case YEAR_MONTH_DAY_H: // "^\\d{4}-\\d{2}-\\d{2}$"
      case YEAR_MONTH_DAY: // "^\\d{8}$"
        setCertainty(Certainty.HIGH);
        handleYearMonthDay(text);
        break;
      case YEAR_MONTH_DAY_HQ: // "^\\d{4}-\\d{2}-\\d{2}\\?$"
      case YEAR_MONTH_DAY_Q: // "^\\d{8}\\?$"
        setCertainty(Certainty.LOW);
        handleYearMonthDay(text);
        break;
      case YEAR_MONTH_DAY_HA: // "^\\d{4}-\\d{2}-\\d{2}\\~$"
      case YEAR_MONTH_DAY_A: // "^\\d{8}\\~$"
        setCertainty(Certainty.MEDIUM);
        handleYearMonthDay(text);
        break;
      case YEAR_RANGE:
        setCertainty(text.contains("?") ? Certainty.LOW : Certainty.MEDIUM);
        String[] dmy2 = text.replace("?", "").split("/");
        calendar.clear();
        set(calendar, Integer.parseInt(dmy2[0]), Calendar.JANUARY, 1);
        setFromDate(calendar);
        calendar.clear();
        set(calendar, Integer.parseInt(dmy2[1]), Calendar.DECEMBER, 31);
        setToDate(calendar);
        break;
      default:
        throw new RuntimeException("Unhandled case: " + edtf);
      }
    }
  }

  /**
   * Handles EDTF for year, month, day format, stripping hyphens, question mark and tilde
   */
  private void handleYearMonthDay(String edtf) {
    String text = edtf.replaceAll("[\\-\\?\\~]", "");
    // must be exactly 8 characters now
    int year = Integer.parseInt(text.substring(0, 4));
    int month = Integer.parseInt(text.substring(4, 6));
    int day = Integer.parseInt(text.substring(6, 8));
    Calendar calendar = getCalendar();
    calendar.clear();
    set(calendar, year, month - 1, day);
    setFromDate(calendar);
    setToDate(calendar);
  }

  private void setFromYear(String edtf) {
    Calendar calendar = getCalendar();
    int year = Integer.parseInt(edtf);

    calendar.clear();
    set(calendar, year, Calendar.JANUARY, 1);
    setFromDate(calendar);

    calendar.clear();
    set(calendar, year, Calendar.DECEMBER, 31);
    setToDate(calendar);
  }

  private void setFromMonthYear(String edtf) {
    Calendar calendar = getCalendar();
    String[] part = edtf.split("-");
    int year = Integer.parseInt(part[0]);
    int month = Integer.parseInt(part[1]) - 1;

    calendar.clear();
    set(calendar, year, month, 1);
    setFromDate(calendar);

    calendar.clear();
    set(calendar, year, month + 1, 1);
    calendar.add(Calendar.DAY_OF_YEAR, -1);
    setToDate(calendar);
  }

  private void setCertainty(Certainty certainty) {
    this.certainty = certainty;
  }

  private void setFromDate(Calendar calendar) {
    fromDate = calendar.getTime();
  }

  private void setToDate(Calendar calendar) {
    toDate = calendar.getTime();
  }

  public int getFromYear() {
    return fromDate != null ? getYear(fromDate) : 0;
  }

  public int getToYear() {
    return toDate != null ? getYear(toDate) : 0;
  }

  private int getYear(Date date) {
    Calendar calendar = getCalendar();
    calendar.setTime(date);
    return calendar.get(Calendar.YEAR);
  }

  public int compareTo(Datable other) {
    if (other == null) {
      return 0;
    }
    int compareFrom = getFromDate().compareTo(other.getFromDate());
    return (compareFrom != 0) ? compareFrom : (getToDate().compareTo(other.getToDate()));
  }

}
