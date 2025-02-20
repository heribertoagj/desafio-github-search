package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    lateinit var editUserName: EditText
    lateinit var btnConfirm: Button
    lateinit var listRepositories: RecyclerView
    lateinit var githubApi: GitHubService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        showUserName()
        setupRetrofit()
        setupListeners()
        getAllReposByUserName()
    }
    fun setupView() {
        editUserName = findViewById(R.id.et_user_name)
        btnConfirm = findViewById(R.id.btn_confirm)
        listRepositories = findViewById(R.id.rv_list_repositories)
    }

    private fun setupListeners() {
        btnConfirm.setOnClickListener {
            val userNameInformed = editUserName.text.toString()
            saveUserLocal(userNameInformed)
            getAllReposByUserName()
        }
    }

    private fun saveUserLocal(userName: String) {
        val userNameShared = getPreferences(Context.MODE_PRIVATE)?: return
        with(userNameShared.edit()) {
            putString(getString(R.string.user_name_informed), userName)
            apply()
        }
    }

    private fun showUserName() {
        var userNameShared = getPreferences(Context.MODE_PRIVATE)
        var userNameInformed =  userNameShared.getString(getString(R.string.user_name_informed)," ")?:""
        editUserName.setText(userNameInformed)
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    fun setupRetrofit() {
        var httpClient = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        githubApi = httpClient.create(GitHubService::class.java)
    }

    fun getAllReposByUserName() {
        githubApi.getAllRepositoriesByUser(editUserName.text.toString()).enqueue(
            object : Callback<List<Repository>> {
                override fun onResponse(
                    call: Call<List<Repository>>,
                    response: Response<List<Repository>>
                ) {
                    if (response.isSuccessful){
                        response.body()?.let {
                            setupAdapter(it)
                        }
                    }
                    else {
                        Log.e("GitApi", "Calling github api error")
                    }
                }
                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    Log.e("GitApi", "Calling github api error")
                }
            }
        )
    }

    fun setupAdapter(list: List<Repository>) {
        var adapter = RepositoryAdapter(list)
        listRepositories.adapter = adapter
        adapter.repoItemLister = { repo ->
            openBrowser(repo.htmlUrl)
        }

        adapter.btnShareLister = {
            shareRepositoryLink(it.htmlUrl)
        }
    }

    fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )
    }
}