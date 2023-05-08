# Paidy Forex Proxy
Implementation of Paidy's Forex Proxy skeleton code

## Assumptions and Limitations
- Support 9 currencies: `AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD`
- OneFrame can only handle 1000 requests per day, expect to return http error when limit exceeded.
- The application server can handle any number of request.

## Implementation
There's 3 main requirements:
1. The service returns an exchange rate when provided with 2 supported currencies
2. The service should support at least 10,000 successful requests per day with 1 API token
3. The rate should not be older than 5 minutes

The 1st requirement can be easily fulfilled by asking OneFrame API.
The 2nd requirement indicates the need to use caching mechanism since OneFrame API can only support 1000 requests per day but here we need 10,000 requests per day.
The 3rd requirement limit the caching time to be less or equal than 5 minutes.

The idea is, everytime the service got a request, it will check the cache.
If the request pair is found in cache, return the value immediately.
If not found, it will fetch the rates from OneFrame API and store it to the cache, then return the requested rate.
Subsequent request for the next 4 minutes period won't need to ask OneFrame API anymore.

Some key decisions:
- `Redis` is used as caching server because it is readily available, good performance, easy to use, and error tested.
- The caching period is set to 4 minutes to accommodate processing delay between hitting OneFrame API and storing to cache.
- A single API call to OneFrame API will request all possible permutations of currency pair to further save number of API calls. 72 of pair combinations should still be enough on a single API call.

## Running
Since the application requires `Redis`, you will need to have it installed on your machine first.
If you don't, the easiest way is to run redis from docker (assuming you already had `docker` installed)

```shell
docker pull redis
docker run -p 6379:6379 redis
```

Start the application
```shell
sbt run
```

To run the test
```shell
sbt test
```

## Possible Improvement
- Fallback when `Redis` is unavailable, possibly an application level cache using `Map`.
- Dockerized application for easier scaling.
- Testing without `unsafeRunAsync`.
- Support for more currencies.
- Add request/response logger.