import random

secret_number = random.randint(1, 100)
max_tries = 5

print("Welcome to the number guessing game!")
print("You have 5 tries to guess any number between 1 and 100")

for attempt in range(1, max_tries + 1):
    guess = int(input(f"Attempt {attempt}: Enter your guess: "))

    if guess == secret_number:
        print(f"Congratulations! You guessed it in {attempt} tries!")
        break
    elif attempt < max_tries:
        if guess < secret_number:
            print("Too low!")
        else:
            print("Too high!")
        print(f"You have {max_tries - attempt} tries left")
    else:
        print(f"Game Over! The number was {secret_number}")