package ru.kata.project.myprila.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.kata.project.myprila.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}