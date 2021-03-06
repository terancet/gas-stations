package repository;


import model.type.CarType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CarTypeRepository extends JpaRepository<CarType, Integer> {
    @Query("select carType from CarType carType where carType.typeName=:name")
    CarType getByName(@Param("name") String name);
}
