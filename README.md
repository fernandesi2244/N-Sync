# Thunderbird-Meeting-Tracker

## Inspiration
I was asked to create this app for my school because of the tedious sign-in process that was taking place every single time
a teacher meeting occurred. All of the teachers had to wait to physically sign in to the meetings, significantly reducing the
time management and potential productivity of the meeting. The Thunderbird Meeting Tracker is an app to help address that issue!

## What it does
Each teacher can create an account on the app, saving their personal information and meeting attendance history to the Back4App
backend framework platform. On the profile page, the app displays the next meeting so that the teacher can be prepared for it. 
In addition to this, there is a separate page that allows the teacher to sign into an available meeting. This sign-in history is
recorded for admin use. Accounts created with admin privileges can schedule meetings with different dates, times, and even 
locations! With these settings, teachers must be present at the meeting location and be there at the right time in order to 
successfully sign in. Admins can also edit these meetings at any time and view the meeting history for each user using the app.
In the end, not only can teachers quickly sign into meetings with the peace of mind that their digital transactions are recorded 
online, but admins can also monitor meeting attendance and control meeting parameters with ease.

[Devpost Link (with screenshots)](https://devpost.com/software/thunderbird-meeting-tracker)

## How I built it
I used Android Studio to code my project in Java. For online database CRUD operations, I utilized the Back4App backend framework
based on the Parse platform. In order to utilize the Google Maps SDK API (and in the future, Google Places API) for both geolocation
and geocoding purposes, I created a Google Cloud Platform account, activated the necessary APIs, and connected them with my project.
I used many resources along the way, most notably the Android Developer and Back4App documentation.

## Challenges I ran into
Most of the challenges I ran into included learning the commands of the Google Maps APIs that I was not previously familiar with.
Additionally, I had to play around with the logic in many parts of my program in order to get the optimal result. Finally, I had
an issue converting ParseObject pointers into their equivalent objects. However, I was able to solve all of these issues with the
help of problem-solving skills and online documentation!

## Accomplishments that I'm proud of
I'm proud of getting so much work done in these last couple of days of the hackathon. I'm especially proud of both my 
location-tracking feature of the app, which determines if a user's location is within a specified margin of error of the meeting
location, as well as the ParseQueries that I designed to optimally display results for the user.

## What I learned
Google Maps APIs, more ParseQuery skills, general Android skills (with various elements and their respective methods/operations),
effectively working with Date manipulation and representation in Android

## What's next for Thunderbird Meeting Tracker
- Push notifications and emails for new meetings
- ~~Log any errors caught (with their appropriate context) to the online database (so that I can actively improve the app)~~
- Add screen/logic for admins to select a meeting and see who attended it (rather than clicking on a user and seeing what meetings
  they attended or looking at the online database dashboard)
