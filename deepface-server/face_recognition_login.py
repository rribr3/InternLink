import cv2
import numpy as np
import os
import json
from datetime import datetime
import logging
from deepface import DeepFace
import tempfile

class DeepFaceLogin:
    def __init__(self, users_db_path="users_faces_deep.json"):
        """
        Initialize DeepFace login system
        """
        self.users_db_path = users_db_path
        self.users_info = {}
        self.registered_faces_dir = "registered_faces"
        
        # Create directory for storing face images
        if not os.path.exists(self.registered_faces_dir):
            os.makedirs(self.registered_faces_dir)
        
        # Load existing data
        self.load_user_data()
        
        # Setup logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)

    def load_user_data(self):
        """Load existing user information"""
        if os.path.exists(self.users_db_path):
            with open(self.users_db_path, 'r') as f:
                self.users_info = json.load(f)

    def save_user_data(self):
        """Save user information"""
        with open(self.users_db_path, 'w') as f:
            json.dump(self.users_info, f, indent=2)

    def register_user(self, username, image_path=None, use_camera=False):
        """
        Register a new user with their face
        """
        if username in self.users_info:
            self.logger.error(f"User {username} already exists!")
            return False

        user_image_path = None
        
        if use_camera:
            user_image_path = self._capture_face_from_camera(username)
        elif image_path and os.path.exists(image_path):
            # Copy image to registered faces directory
            import shutil
            user_image_path = os.path.join(self.registered_faces_dir, f"{username}.jpg")
            shutil.copy2(image_path, user_image_path)
        else:
            self.logger.error("Please provide valid image path or use camera")
            return False

        if user_image_path and os.path.exists(user_image_path):
            # Verify face can be detected
            try:
                # Try to extract embeddings to verify face detection
                embedding = DeepFace.represent(img_path=user_image_path, model_name='VGG-Face')
                
                # Store user info
                self.users_info[username] = {
                    'image_path': user_image_path,
                    'registered_date': datetime.now().isoformat(),
                    'login_count': 0,
                    'last_login': None
                }
                
                self.save_user_data()
                self.logger.info(f"User {username} registered successfully!")
                return True
                
            except Exception as e:
                self.logger.error(f"Face detection failed: {e}")
                if os.path.exists(user_image_path):
                    os.remove(user_image_path)
                return False
        else:
            self.logger.error("Failed to save user image!")
            return False

    def _capture_face_from_camera(self, username):
        """Capture face from camera for registration"""
        cap = cv2.VideoCapture(0)
        
        if not cap.isOpened():
            self.logger.error("Cannot access camera")
            return None
        
        print("Position your face in the camera and press SPACE to capture, ESC to cancel")
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            cv2.putText(frame, "Press SPACE to capture, ESC to cancel", 
                       (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            cv2.putText(frame, f"Registering: {username}", 
                       (10, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)
            
            cv2.imshow('Face Registration', frame)
            
            key = cv2.waitKey(1) & 0xFF
            if key == ord(' '):  # Space to capture
                user_image_path = os.path.join(self.registered_faces_dir, f"{username}.jpg")
                cv2.imwrite(user_image_path, frame)
                cap.release()
                cv2.destroyAllWindows()
                return user_image_path
            elif key == 27:  # ESC to cancel
                break
        
        cap.release()
        cv2.destroyAllWindows()
        return None

    def authenticate_user(self, image_path=None, use_camera=False, threshold=0.6):
        """
        Authenticate user using face recognition
        
        Args:
            image_path: Path to image for authentication
            use_camera: Whether to use camera for authentication
            threshold: Similarity threshold (lower = stricter)
        
        Returns:
            tuple: (success, username, confidence)
        """
        if len(self.users_info) == 0:
            self.logger.error("No users registered yet!")
            return False, None, 0

        test_image_path = None
        
        if use_camera:
            test_image_path = self._capture_test_image()
        elif image_path and os.path.exists(image_path):
            test_image_path = image_path
        else:
            self.logger.error("Please provide valid image path or use camera")
            return False, None, 0

        if not test_image_path:
            return False, None, 0

        try:
            # Get the best match
            best_match = None
            best_distance = float('inf')
            
            for username, user_data in self.users_info.items():
                registered_image_path = user_data['image_path']
                
                if not os.path.exists(registered_image_path):
                    continue
                
                try:
                    # Verify faces using DeepFace
                    result = DeepFace.verify(
                        img1_path=test_image_path,
                        img2_path=registered_image_path,
                        model_name='VGG-Face'
                    )
                    
                    distance = result['distance']
                    is_verified = result['verified']
                    
                    if is_verified and distance < best_distance:
                        best_distance = distance
                        best_match = username
                        
                except Exception as e:
                    self.logger.warning(f"Verification failed for {username}: {e}")
                    continue
            
            # Clean up temporary file if created
            if use_camera and test_image_path:
                os.remove(test_image_path)
            
            if best_match and best_distance < threshold:
                # Update user info
                self.users_info[best_match]['login_count'] += 1
                self.users_info[best_match]['last_login'] = datetime.now().isoformat()
                self.save_user_data()
                
                confidence = 1 - best_distance  # Convert distance to confidence
                return True, best_match, confidence
            else:
                return False, None, 0
                
        except Exception as e:
            self.logger.error(f"Authentication error: {e}")
            return False, None, 0

    def _capture_test_image(self):
        """Capture image from camera for authentication"""
        cap = cv2.VideoCapture(0)
        
        if not cap.isOpened():
            self.logger.error("Cannot access camera")
            return None
        
        print("Look at the camera for authentication. Press SPACE to capture, ESC to cancel")
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            cv2.putText(frame, "Press SPACE to authenticate, ESC to cancel", 
                       (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            
            cv2.imshow('Face Authentication', frame)
            
            key = cv2.waitKey(1) & 0xFF
            if key == ord(' '):  # Space to capture
                # Save to temporary file
                temp_file = tempfile.NamedTemporaryFile(suffix='.jpg', delete=False)
                temp_path = temp_file.name
                temp_file.close()
                
                cv2.imwrite(temp_path, frame)
                cap.release()
                cv2.destroyAllWindows()
                return temp_path
            elif key == 27:  # ESC to cancel
                break
        
        cap.release()
        cv2.destroyAllWindows()
        return None

    def list_users(self):
        """List all registered users"""
        if not self.users_info:
            print("No users registered")
            return
        
        print("\nRegistered Users:")
        print("-" * 60)
        for username, info in self.users_info.items():
            print(f"Username: {username}")
            print(f"Registered: {info['registered_date']}")
            print(f"Login Count: {info['login_count']}")
            print(f"Last Login: {info['last_login']}")
            print(f"Image Path: {info['image_path']}")
            print("-" * 60)

    def delete_user(self, username):
        """Delete a user from the system"""
        if username not in self.users_info:
            self.logger.error(f"User {username} not found!")
            return False
        
        # Remove user image
        image_path = self.users_info[username]['image_path']
        if os.path.exists(image_path):
            os.remove(image_path)
        
        # Remove user info
        del self.users_info[username]
        self.save_user_data()
        
        self.logger.info(f"User {username} deleted successfully!")
        return True


# Example usage
def main():
    """Demo of the DeepFace login system"""
    login_system = DeepFaceLogin()
    
    while True:
        print("\n=== DeepFace Recognition Login System ===")
        print("1. Register new user")
        print("2. Login with face")
        print("3. List users")
        print("4. Delete user")
        print("5. Exit")
        
        choice = input("Enter your choice (1-5): ").strip()
        
        if choice == '1':
            username = input("Enter username: ").strip()
            method = input("Use camera (c) or image file (f)? ").strip().lower()
            
            if method == 'c':
                success = login_system.register_user(username, use_camera=True)
            else:
                image_path = input("Enter path to image file: ").strip()
                success = login_system.register_user(username, image_path=image_path)
            
            if success:
                print(f"User {username} registered successfully!")
            else:
                print("Registration failed!")
        
        elif choice == '2':
            method = input("Use camera (c) or image file (f)? ").strip().lower()
            
            if method == 'c':
                success, username, confidence = login_system.authenticate_user(use_camera=True)
            else:
                image_path = input("Enter path to image file: ").strip()
                success, username, confidence = login_system.authenticate_user(image_path=image_path)
            
            if success:
                print(f"Login successful! Welcome {username} (Confidence: {confidence:.3f})")
            else:
                print("Authentication failed!")
        
        elif choice == '3':
            login_system.list_users()
        
        elif choice == '4':
            username = input("Enter username to delete: ").strip()
            login_system.delete_user(username)
        
        elif choice == '5':
            print("Goodbye!")
            break
        
        else:
            print("Invalid choice!")


if __name__ == "__main__":
    main()