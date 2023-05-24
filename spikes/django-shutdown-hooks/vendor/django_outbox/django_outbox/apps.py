from django.apps import AppConfig
import signal
import time


class OldDjangoOutboxConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'django_outbox'
    default = True

    def __init__(self, *args):
        self._producers = dict()

        super().__init__(*args)


    def ready(self):
        print("REGISTERING SHUTDOWN HOOK")
        def handle_sigterm(signum, frame):
            print(f"Received {signum}. Shutting down gracefully...")

            for k, v in self.producers().items():
                print(f"Shutting down {k}")
                time.sleep(1)
 
            exit(0)

        signals = [
            signal.SIGTERM,
            signal.SIGINT,
        ]

        for sig in signals:
            print(f"  REGISTERING {sig}")
            signal.signal(sig, handle_sigterm)

        print("SHUTDOWN HOOK REGISTERED")

        print("ADDING PRODUCERS")
        for s in ["foo", "bar"]:
            print(f"  ADDING PRODUCER {s}")
            self.configure_producer(s)


    def get_producer(self, name):
        return self._producers[name]


    def configure_producer(self, name, *args):
        self._producers[name] = name
        return self.get_producer(name)


    def producers(self):
        return self._producers
