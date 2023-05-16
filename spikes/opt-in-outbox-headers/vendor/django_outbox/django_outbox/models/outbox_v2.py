from ..models import OUTBOX_TABLE
from .basic_outbox import BasicDjangoOutbox

class DjangoOutbox(BasicDjangoOutbox):
    version = models.CharField(max_length=255)
    class Meta:
        db_table = OUTBOX_TABLE

