from django.apps import AppConfig


class OldDjangoOutboxConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'django_outbox'
    default = True

    def ready(self):
        print("OLD AND BUSTED")


class NewDjangoOutboxConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'django_outbox'

    def ready(self):
        print("NEW HOTNESS")
