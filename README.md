# hyperskill-web-quiz-engine
Simple Web Quiz Engine (REST API)
A simple Web Quiz Engine is implemented via Spring Boot.
Engine uses JSON to exchange data with client side, and capable to process REST requests: GET, POST, PUT, DELETE and PATCH.
The data are stored in file-stored H2 Database with relational tables.
PATCH handler is implemented using fge library, but it contains some bugs.
There's also basic HTTP user authorization.
General idea is the following:

User can register and after that he is authenticated to perform the following operations:
- POST a new quiz, which is saved in a database;
- solve any quiz (even posted by other users)
- GET all quizzes (even posted by other users) with pagination and sorting
- GET all quizzes solved by him/her
- update, patch or delete any quiz which he added to server (PUT, PATCH and DELETE requests)

Future plans:

1. Add HTML pages rendering to make the app more user-friendly.
2. Add admin users to manage the app.
3. Add import from other sources (files, other platforms with open API).

