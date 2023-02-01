import sys
# import tracemalloc
import time
# from memory_profiler import profile
# from io import StringIO
# import cProfile
# from cProfile import Profile
# from pstate import Stats

# @profile
def solve(a, b) :
  c = a + b
  return c

def main():
  start_time = time.time()
#   tracemalloc.start()
#   profiler = Profile()
#   profiler.run('solve()')
#   stats = Stats(profiler)
#   stats.strip_dirs()
#   stats.sort_stats('cumulative')
#   stats.print_stats()
#   profiler = cProfile.Profile()
#   profiler.enable()

  solve(2, 45)

#   profiler.disable()
#
#   output = StringIO()
#   sortby = 'cumulative'
#   profiler.print_stats(sortby=sortby, stream=output)
#
#   result = output.getvalue()
#   print(result)


  print(solve(2,3))
#   tracemalloc.start()
#   print(tracemalloc.get_traced_memory())
  s1 = set({1, 2, 3})
  s2 = set({2, 3, 4})
  print(s1.intersection(s2))
#   snapshot = tracemalloc.take_snapshot()
#   top_stats = snapshot.statistics('lineno')
#   for stat in top_stats[:10]:
#     print(stat)


  end_time = time.time()
  secs = end_time - start_time
  print("time : ", secs)

  return s1.intersection(s2)
# if __name__ == "__main__":
# 	main()