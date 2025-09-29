import requests

class MyApi:
    def get_user(self, user_id: int) -> dict:
        url = f"https://jsonplaceholder.typicode.com/users/{user_id}"
        response = requests.get(url)
        response.raise_for_status()
        return response.json()

import polyglot
polyglot.export_value('MyApi', MyApi)