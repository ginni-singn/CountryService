
# Country Service

## Run
mvn spring-boot:run

## API
GET /countries/{code}

## Example
http://localhost:8080/countries/US

## Test
mvn test

## Design
- Layered architecture
- RestTemplate injected
- Global exception handling
- Defensive null checks
- Unit tests added
