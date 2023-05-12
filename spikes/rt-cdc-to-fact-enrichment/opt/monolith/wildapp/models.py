from django.db import models
import uuid

class Primary(models.Model):
    name = models.CharField(max_length=255)
    canonical_id = models.UUIDField(default=uuid.uuid4)

class Secondary(models.Model):
    name = models.CharField(max_length=255)
    parent = models.ForeignKey(Primary, on_delete=models.CASCADE)

class Tertiary(models.Model):
    name = models.CharField(max_length=255)
    parent = models.ForeignKey(Secondary, on_delete=models.CASCADE)
