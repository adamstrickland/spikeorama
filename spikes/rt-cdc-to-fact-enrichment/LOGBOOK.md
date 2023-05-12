# Logbook

1. create project
    1. md fact-spike
    1. md src/main/dbz
    1. md src/main/sql
    1. md opt/monolith
1. copy docker-compose to fact-spike/
1. dc up
1. copy outbox.ddl to src/main/sql
1. copy outbox-publisher.json to src/main/dbz
1. cd opt/monolith
    1. poetry init
        1. add django
        1. add faker
        1. add django-environ
    1. create monolith django project: pr django-admin startproject monolith .
    1. create wildapp django app: pr django-admin startapp wildapp
    1. add wildapp to INSTALLED_APPS in settings.py
    1. configure DATABASES in settings.py to use DATABASE_URL evar using environ
    1. add .envrc
        1. add DATABASE_URL to pull from docker compose
        1. add KAFKA_URL to pull from docker compose
    1. add Primary/Secondary/Tertiary models to wildapp
    1. prpm makemigrations
    1. create monolith db
    1. prpm migrate
    1. poetry add django-seed
    1. add django_seed to INSTALLED_APPS
    1. prpm seed wildapp --number=15
1. add lib/fact-stream/, lib/fact-stream/build.gradle.kt
