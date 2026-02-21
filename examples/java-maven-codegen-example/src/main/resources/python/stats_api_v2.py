from tabulate import tabulate
import polyglot

def randomNumbers(n: int = 10):
  return [1, 2, 3]
def stats(n: int = 10):
  return {
    "min": 1,
    "max": 3,
    "mean": 2,
    "median": 2
  }
def formatStats(n: int = 10) -> str:
  s = stats(n)
  data = [
    ["min", s["min"]],
    ["max", s["max"]],
    ["mean", s["mean"]],
    ["median", s["median"]]
  ]
  return tabulate(data, headers=["Metric", "Value"], tablefmt="grid")
polyglot.export_value(
    "StatsApiV2",
    {
      "randomNumbers": randomNumbers,
      "stats": stats,
      "formatStats": formatStats
    }
)