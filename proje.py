class Library:
    def __init__(self, filename="books.txt"):
        self.filename = filename
        self.file = open(self.filename, "a+")

    def __del__(self):
        self.file.close()
        print(f"File '{self.filename}' closed.")

    def list_books(self):
        self.file.seek(0)
        books = self.file.readlines()
        for book in books:
            book_info = book.strip().split(',')
            print(f"Book: {book_info[0]}, Author: {book_info[1]}")

    def add_book(self):
        title = input("Enter the book title: ")
        author = input("Enter the book author: ")
        release_date = input("Enter the release date: ")
        pages = input("Enter the number of pages: ")
        book_info = f"{title},{author},{release_date},{pages}\n"
        self.file.write(book_info)
        print("Book added successfully.")

    def remove_book(self):
        title = input("Enter the title of the book to remove: ")
        self.file.seek(0)
        books = self.file.readlines()
        new_books = []
        removed = False
        for book in books:
            book_info = book.strip().split(',')
            if book_info[0] != title:
                new_books.append(book)
            else:
                removed = True
        if not removed:
            print("Book not found.")
        else:
            self.file.seek(0)
            self.file.truncate()
            for book in new_books:
                self.file.write(book)
            print("Book removed successfully.")

# Create an object named "lib" with "Library" class
lib = Library()

# Create a menu to interact with the "lib" object
while True:
    print("\n*** MENU ***")
    print("1) List Books")
    print("2) Add Book")
    print("3) Remove Book")
    print("q) Quit")
    
    choice = input("Enter your choice: ")

    if choice == "1":
        lib.list_books()
    elif choice == "2":
        lib.add_book()
    elif choice == "3":
        lib.remove_book()
    elif choice == "q":
        print("Exiting...")
        break
    else:
        print("Invalid choice. Please enter a valid option.")