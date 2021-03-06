package br.com.zup.autores

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.*
import io.micronaut.http.annotation.*
import io.micronaut.http.uri.UriBuilder
import io.micronaut.validation.Validated
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Controller(value = "/api/autores")
class AutorController(
  val autorRepository: AutorRepository,
  val enderecoCliente: EnderecoClient
) {

  @Post
  @Transactional
  fun create(@Body @Valid request: AutorRequest): HttpResponse<Any> {
    val enderecoResponse = enderecoCliente.consulta(request.cep)

    val autor = request.toModel(enderecoResponse.body()!!)
    autorRepository.save(autor)

    val uri = UriBuilder.of("/api/autores/{id}")
      .expand(mutableMapOf(Pair("id", autor.id)))

    return created(uri)
  }

  @Get
  @Transactional
  fun read(): HttpResponse<List<AutorResponse>> {
    val autores: MutableIterable<Autor> = autorRepository.findAll()
    val resposta = autores.map { autor -> AutorResponse(autor.nome, autor.email, autor.descricao) }
    return ok(resposta)
  }

  @Get("/buscarPor")
  @Transactional
  fun findByEmail(@QueryValue(value = "") email: String): HttpResponse<Any> {
    if (email.isBlank()) {
      val autores = autorRepository.findAll()
      val resposta = autores.map { autor -> AutorResponse(autor.nome, autor.email, autor.descricao) }
      return ok(resposta)
    }
    val possivelAutor = autorRepository.buscarByEmailQueryJpql(email)
    if (possivelAutor.isEmpty) {
      return notFound()
    }
    val autor = possivelAutor.get()
    return ok(AutorResponse(autor.nome, autor.email, autor.descricao))
  }

  @Patch("/{id}")
  @Transactional
  fun update(@PathVariable id: Long, descricao: String): HttpResponse<Any> {
    val possivelAutor = autorRepository.findById(id)

    if (possivelAutor.isEmpty) {
      return notFound()
    }

    val autor = possivelAutor.get()
    autor.descricao = descricao

    /**
     * Com o @Transational não preciso fazer o update usando o repository de autor
     * explicitamente pois caso tenha alguma modificação no estado da minha
     * classe será feito de forma automatica o update pelo hibernate no final da
     * transação.
     * */
    // autorRepository.update(autor)

    return ok(AutorResponse(autor.nome, autor.email, autor.descricao))
  }

  @Delete("/{id}")
  @Transactional
  fun delete(@PathVariable id: Long): HttpResponse<Any> {
    val possivelAutor = autorRepository.findById(id)

    if (possivelAutor.isEmpty) {
      return notFound()
    }
    autorRepository.delete(possivelAutor.get()) //<- também posso fazer assim
    //autorRepository.deleteById(id)

    return noContent()
  }
}