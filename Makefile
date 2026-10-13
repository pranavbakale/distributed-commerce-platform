.PHONY: test build up down logs clean

test:
	mvn verify

build:
	mvn package

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f --tail=200

clean:
	mvn clean

