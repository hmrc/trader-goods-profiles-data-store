
# trader-goods-profiles-data-store

The service will be responsible for creating, retrieving, updating and deactivating new records / profile as opposed to getting it from B&T database.

## Set profile data

### Example request

POST request to the following endpoint

```
https://<host>/trader-goods-profiles-data-store/traders/:eori/profile
```

`ProfileRequest`:
```json
{
  "actorId": "GB123456789010",
  "ukimsNumber": "XIUKIM47699357400020231115081800",
  "nirmsNumber": "RMS-GB-123456",
  "niphlNumber": "S12345"
}
```
## Get profile data

| EXPECTED RESPONSE     | STATUS CODE |
|-----------------------|-------------|
| OK                    | 200         |
| Internal server error | 500         |
| Not found             | 404         |


### Example request

GET request to the following endpoint

```
https://<host>/trader-goods-profiles-data-store/customs/traders/goods-profiles/:eori  
```
### Example response:
`ProfileResponse`:
```json
{
  "eori": "GB123456789010",
  "actorId": "GB123456789010",
  "ukimsNumber": "XIUKIM47699357400020231115081800",
  "nirmsNumber": "RMS-GB-123456",
  "niphlNumber": "S12345"
}
```

## Does profile exist

| EXPECTED RESPONSE     | STATUS CODE |
|-----------------------|-------------|
| OK                    | 200         |
| Internal server error | 500         |
| Not found             | 404         |


### Example request

HEAD request to the following endpoint

```
https://<host>/trader-goods-profiles-data-store/traders/:eori/profile
```

### Setup

Run this service from your console using: `sbt run`

### Testing

Run this to check unit tests: `sbt test`

Run this to check integration tests: `sbt it/test`

Run this to check all code coverage and get report: `sbt clean coverage test it/test coverageReport`

### Formatting

Run this to check all scala files are formatted: `sbt scalafmtCheckAll`

Run this to format non-test scala files: `sbt scalafmt`

Run this to format test scala files: `sbt test:scalafmt`



### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

