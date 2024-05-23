package com.herokuapp.restfulbooker;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApiTests {

    private static final Logger logger = LogManager.getLogger("ApiTests.class");
    private List<BookingInfo> bookingInfos = new ArrayList<>();
    private Response response;
    private final String requestUrl = "https://restful-booker.herokuapp.com/booking/";

    private static class BookingInfo {
        int bookingId;
        double newPrice;

        BookingInfo(int bookingId, double newPrice) {
            this.bookingId = bookingId;
            this.newPrice = newPrice;
        }

    }

    @Test
    public void createNewBookingsTest() throws IOException, CsvException {

        // Read data from CSV file
        CSVReader csvReader = new CSVReader(new FileReader("src/main/resources/test-data.csv"));
        List<String[]> records = csvReader.readAll();

        // Skip header
        records.remove(0);

        //Iterate
        for (String[] record : records) {
            String firstname = record[0];
            String lastname = record[1];
            int totalprice = Integer.parseInt(record[2]);
            boolean depositpaid = Boolean.parseBoolean(record[3]);
            String checkin = record[4];
            String checkout = record[5];
            String additionalneeds = record[6];
            double newPrice = Double.parseDouble(record[7]);

            JSONObject body = new JSONObject();
            body.put("firstname", firstname);
            body.put("lastname", lastname);
            body.put("totalprice", totalprice);
            body.put("depositpaid", depositpaid);
            JSONObject bookingdates = new JSONObject();
            bookingdates.put("checkin", checkin);
            bookingdates.put("checkout", checkout);
            body.put("bookingdates", bookingdates);
            if (additionalneeds != null && !additionalneeds.trim().isEmpty()) {
                body.put("additionalneeds", additionalneeds);
            }

            //Send Post request and get back the response and log
            response = RestAssured.given().contentType(ContentType.JSON).body(body.toString()).post(requestUrl);
            logger.info("New Bookings made: {}", response.getBody().asPrettyString());

            //Store booking ids in list
            int bookingId = response.jsonPath().getInt("bookingid");
            bookingInfos.add(new BookingInfo(bookingId, newPrice));

            //Verify firstname in the response payload is same as the one in the request body
            Assert.assertEquals(response.jsonPath().get("booking.firstname"), record[0], "Firstname in the response does not match with the request payload");
        }
    }

    @Test
    public void logAllAvailableIdsTest() {
        //Send get request and get back the response
        response = RestAssured.get(requestUrl);

        //Log all bookingId's
        logger.info("Showing all Booking Id's: {}", response.getBody().asPrettyString());

        //Verify statuscode is 200
        Assert.assertEquals(response.getStatusCode(), 200, "Status is not 200");
    }

    @Test
    public void updateExistingBookingsWithTotalPrice() throws IOException, CsvException {

        for (BookingInfo info : bookingInfos) {
            JSONObject body = new JSONObject();
            body.put("totalprice", info.newPrice);
            response = RestAssured.given().auth().preemptive().basic("admin", "password123").contentType(ContentType.JSON).body(body.toString()).patch(requestUrl + info.bookingId);
            logger.info("Updated totalprice for booking id : " + info.bookingId + " {}", response.getBody().asPrettyString());

            //Verify updated totalprice is seen in the response
            Assert.assertEquals((int) response.jsonPath().get("totalprice"), info.newPrice, "Updated totalprice does not match");

        }
    }

    @Test
    public void deleteExistingBooking() {
        int bookingId = bookingInfos.get(0).bookingId;

        response = RestAssured.given().auth().preemptive().basic("admin", "password123").delete(requestUrl + bookingId);
        logger.info("Booking deleted for booking id : " + bookingInfos.get(0).bookingId + " {}", "Delete API response : " + response.getBody().asPrettyString());

        //Verify statuscode is 201
        Assert.assertEquals(response.getStatusCode(), 201, "Status is not 201");
    }
}

