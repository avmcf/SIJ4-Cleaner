package com.vconsulte.sij.cleaner;

//***************************************************************************************************
//Cleaner: Rotina de exclusão de publicações do diario oficial 	
//
//
//	versao 1.0.0 	- 23 de Novembro de 2018
//					Versao Inicial
//
//	versao 1.0.1 	- 17 de Dezembro de 2018
//					Correção na finalização da rotina e exclusão da pasta da edicao selecionada
//
//	versao 1.1.0 	- 11 de Maio de 2020
//					Centralização da configuração 
//
//	versao 2.6 		- 10 de Abril de 2021
//					- Equalização na utilização do método Comuns.apresentamensagem
//
//---------------------------------------------------------------------------------------------------------------------------
//
// V&C Consultoria Ltda.
// Autor: Arlindo Viana.
//
//***************************************************************************************************

import java.util.List;

import javax.swing.*;
//import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
//import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.QueryResult;
//import org.apache.chemistry.opencmis.client.api.Folder;
//import org.apache.chemistry.opencmis.client.api.ItemIterable;
//import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
//import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyData;

import com.vconsulte.sij.base.*;

public abstract class Cleaner extends JPanel implements ActionListener {

	// parametros de configuração
	static String usuario;						// usuario alfresco
	static String password;						// password alfresco
	static String pastaCarregamento;			// Pasta alfresco que recebe as publicacoes
	static String tipoDocumento;				// Tipo do documento no Alfresco
	static String versaoCleaner;				// Versão do Cleaner
	static String pastaSaida;					// pasta onde são gravados as publicacoes
	static String tipoProcessamento;			// Tipo do processamento DESKTOP ou Batch
	static String pastaEdicoes;					// De onde ler as edicoes.pdf
	static String tipoArquivoSaida;				// tipo arquivo saida PDF ou Texto
	static String pastaDeEdicoes;				// tipo arquivo saida PDF ou Texto
	static String pastaLixo;
	
	//static MsgWindow msgWindow = new MsgWindow();
	static InterfaceServidor conexao = new InterfaceServidor();
	static Configuracao configuracao = new Configuracao();
	static SelecionaDO selecionaEdicao = new SelecionaDO();
	static BaixaConteudo baixaEdicoes = new BaixaConteudo();
	static Comuns comuns = new Comuns();

	static List <String> log = new ArrayList<String>();
	static List <String> listaDeEdicoes = new ArrayList<String>();
	
	static Session sessao;
	static Edital publicacao = new Edital();
	static Base base = new Edital();
	
	static String cliente;
	static String tipoConexao;
	static String sysOp;
	static String url;
	static String logFolder;
	static String pastaDePDFs;
	static String dataBase = "";
	static String tipoLimpeza = "";

	static int k = 0;				// testes
	
	Map<String, String> edicoesParaExcluir = new HashMap<String, String>();
		
    public static void main(String[] args) throws Exception{
	
    	tipoLimpeza = args[0].toString();
    	dataBase = args[1].toString();

    	String dataLimite = "";
    	Configuracao.carregaConfig();
    	usuario = com.vconsulte.sij.base.Parametros.USUARIO;
		password = com.vconsulte.sij.base.Parametros.PASSWORD;
		versaoCleaner = com.vconsulte.sij.base.Parametros.VERSAOCLEANER;
		cliente = com.vconsulte.sij.base.Parametros.CLIENTE;	
		tipoConexao = com.vconsulte.sij.base.Parametros.CONEXAO;
		sysOp = com.vconsulte.sij.base.Parametros.SYSOP;
		url = com.vconsulte.sij.base.Parametros.URL;
		logFolder = com.vconsulte.sij.base.Parametros.LOGFOLDER;
		tipoProcessamento = com.vconsulte.sij.base.Parametros.TIPOPROCESSAMENTO;
		pastaEdicoes = com.vconsulte.sij.base.Parametros.PASTACARREGAMENTO;
		pastaDePDFs = com.vconsulte.sij.base.Parametros.PASTADEEDICOES;
		pastaLixo = "/User Homes/sij/lixo";
		
		Comuns.apresentaMenssagem("-----------------------------------------------------------------------------",tipoProcessamento, "informativa", null);
		Comuns.apresentaMenssagem("Cleaner " + versaoCleaner + " - Limpeza de Publicações.", tipoProcessamento, "informativa", null);
		Comuns.apresentaMenssagem("-----------------------------------------------------------------------------",tipoProcessamento, "informativa", null);
		Comuns.apresentaMenssagem("\t\tParametros do processamento " , tipoProcessamento, "informativa", null);
		Comuns.apresentaMenssagem("\tTipo Limpeza: " + tipoLimpeza, tipoProcessamento , "informativa", null);
		Comuns.apresentaMenssagem("\tData Base: " + dataBase, tipoProcessamento, "informativa", null);
		Comuns.apresentaMenssagem("-----------------------------------------------------------------", tipoProcessamento,"informativa", null);
		
		if (!conectaServidor()) { 
			Comuns.apresentaMenssagem("Falha na conexão com o Servidor.", tipoProcessamento, "erro", null);
			finalizaProcesso();
		}
	
		if(tipoLimpeza.equals("lixo")) {
			pastaLixo = conexao.getFolderId(sessao, "lixo");
			listaDeEdicoes = conexao.listaEdicoesDoLixo(sessao, pastaLixo);
		} else {
		//	dataLimite = obtemDataLimite();
		//	listaDeEdicoes = conexao.listarEdicoesPorEdicao(sessao, dataLimite);
			listaDeEdicoes = conexao.listarEdicoesPorEdicao(sessao, dataBase);
		}
		
//		dataLimite = obtemDataLimite();
//		listaDeEdicoes = conexao.listarEdicoesPorEdicao(sessao, dataLimite);
		
		if(listaDeEdicoes.size()>0) {
			excluirPublicacoes();
		} else {
			Comuns.apresentaMenssagem("Não há publicações para excluir." + "", tipoProcessamento, "informativa", null);
		}
		finalizaProcesso();
    }
    
    public static void excluirPublicacoes() throws Exception {
    	int ind = 0;
    	int contador = 0;
    	String pastaApagada = "";
    	List <String> pasta = new ArrayList<String>();
    	String docApagado = "";
    	String idDoc = "";
    	List <String> publicacoesExcluir = new ArrayList<String>();
    	List <String> pastaExclir = new ArrayList<String>();
    	int qtd = 1;
    	Comuns.apresentaMenssagem("Início da exclusão." + "", tipoProcessamento, "informativa", null);

		for (int ix=0; ix<=listaDeEdicoes.size()-1; ix++) {		
			pasta = conexao.getFolderInfo(sessao, listaDeEdicoes.get(ix));
			pastaApagada = pasta.get(3);
			Comuns.apresentaMenssagem("Pasta apagada: " + pastaApagada, tipoProcessamento, "informativa", null);
			publicacoesExcluir = conexao.localizaPublicacoesPorPasta(sessao, listaDeEdicoes.get(ix));
			if(publicacoesExcluir.size() > 0) {
				contador = 0;
				while(publicacoesExcluir.size() > 0 ) {
					ind = publicacoesExcluir.size()-1;
					System.out.println("\nNovo loop\n");
					while(ind >= 0) {
						idDoc = conexao.getFileId(sessao, publicacoesExcluir.get(ind));
						if(idDoc != null) {
							docApagado = conexao.getNomeDocumento(sessao, publicacoesExcluir.get(ind));
							Comuns.apresentaMenssagem(ind + "/" + contador + " - Documento apagado: " + docApagado, tipoProcessamento, "informativa", null);
							conexao.excluiPublicacao(sessao, publicacoesExcluir.get(ind));
						//	System.out.println(ind + "/" + contador + " - " + publicacoesExcluir.get(ind));
							ind--;
							if(ind <0) {
								k++;
								break;
							}
						}
												contador++;
						k++;
					}
					publicacoesExcluir = conexao.localizaPublicacoesPorPasta(sessao, listaDeEdicoes.get(ix));
					ind = publicacoesExcluir.size()-1;
					if(ind <0) {
						k++;
						break;
					}
					k++;
				}
			}	
			InterfaceServidor.excluiPasta(sessao, listaDeEdicoes.get(ix));
			k++;
			fechaConexao();
			if (!conectaServidor()) { 
				Comuns.apresentaMenssagem("Falha na conexão com o Servidor.", tipoProcessamento, "erro", null);
				finalizaProcesso();
			}
			k++;			
		}
		k++;
    }
	
	private static void finalizaProcesso() throws IOException {
		
		if(tipoProcessamento.equals("DESKTOP")){
			Comuns.apresentaMenssagem("Fim do Processamento",tipoProcessamento, "final",null);
		} else if(tipoProcessamento.equals("BATCH")) {
			Comuns.apresentaMenssagem("Fim do Processamento", tipoProcessamento, "informativa", null);
		}
        System.exit(0);
	}
    
    private static boolean conectaServidor() throws IOException {
		registraLog("Conexão com o servidor.\n");
		conexao.setUser(usuario);
		conexao.setPassword(password);
		conexao.setUrl(url);
		sessao = InterfaceServidor.serverConnect(); 
		if (sessao == null) {
			registraLog("Falha na conexao com o servidor");
			return false;
		}
		registraLog("Conexão com o servidor estabelecida com sucesso.");
		return true;
	}
    
    private static boolean fechaConexao() throws IOException {
  		registraLog("Fechando conexão com o servidor.\n");
  		Comuns.apresentaMenssagem("Conexão com o servidor encerada.", tipoProcessamento, "informativa", null);
  		sessao.clear();
  		sessao = null;
  		if (sessao != null) {
  			registraLog("Falha no fechamento da conexao com o servidor");
  			return false;
  		}
  		registraLog("Conexão com o servidor encerrada.");
  		return true;
  	}
    
	private static void registraLog(String registroLog) {
		log.add(obtemHrAtual() + " - " + registroLog);
	}
	
	private static String obtemHrAtual() {

		String hr = "";
		String mn = "";
		String sg = "";
		Calendar data = Calendar.getInstance();
		hr = Integer.toString(data.get(Calendar.HOUR_OF_DAY));
		mn = Integer.toString(data.get(Calendar.MINUTE));
		sg = Integer.toString(data.get(Calendar.SECOND));

		return completaEsquerda(hr,'0',2)+":"+completaEsquerda(mn,'0',2)+":"+completaEsquerda(sg, '0', 2);
	}
	
	private static String completaEsquerda(String value, char c, int size) {
		String result = value;
		while (result.length() < size) {
			result = c + result;
		}
		return result;
	}

}
