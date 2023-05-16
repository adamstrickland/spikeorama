from django.db import models

class BasicDjangoOutbox(models.Model):
    topic = models.CharField(max_length=255)
    data = models.JSONField(null=False, blank=False, editable=False)

    class Meta:
        abstract = True
