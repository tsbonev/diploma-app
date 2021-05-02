package com.tsbonev.cqrs.backend.view.mysql

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface MysqlProductView : CrudRepository<ProductViewEntity, String>

@Entity
@Table(name = "product_view")
data class ProductViewEntity (
	@Id val productId: String,
	val productName: String,
	val number: Long
)