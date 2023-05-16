from django.apps import AppConfig


class OldOutboxConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'outbox'
    default = True


class NewOutboxConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'outbox'
