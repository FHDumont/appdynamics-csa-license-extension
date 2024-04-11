package com.appdynamics.extensions.csalicense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.appdynamics.extensions.csalicense.model.Node;

public class GenericTests {

	public static void main(String... args) {

		try {

			List<String> listNodes = new ArrayList<>();
			HashMap<String, String> hashNodes = new HashMap<>();

			listNodes.add("pxl1mov1bavv");
			listNodes.add("pxl1mov1bavv-3dsd-d3sd-10dlfd03ldk03dl");
			listNodes.add("pxl1mov1bavv-4dsd-d3sd-10dlfd03ldk03dl");
			listNodes.add("pxl1mov1bavv-3dsd-d3sd-10dlfd03ldk03dx");
			listNodes.add("pxl1mov1bavv-54sd-d3sd-10dlfd03ldk03dl");
			listNodes.add("pxl1mov1bavv-3dsd-d3sc-10dlfd03ldk03dl");
			listNodes.add("pxl2mov1baxv-dedsd-d3sc-10dlfd03ldk03dl");
			listNodes.add("pxl2mov1baxv-deddd-d3sc-10dlfd03ldk03dl");
			listNodes.add("pxl2mov1baxv-dedsd-d3sc-10dlfd03ldk03dl");
			listNodes.add("pxl2mov1baxv-d2dsd-d3sc-10dlfd03ldk03dl");
			listNodes.add("pxl2mov1baxv-1edsd-d3sc-10dlfd03ldk03dl");
			listNodes.add("pxl2mov1gacc-4essd-d3sc-10dlfd03ldk03dl");

			for (String nodeName : listNodes) {
				int indexHifen = nodeName.indexOf("-");
				String newNodeName = nodeName;
				if (indexHifen != -1) {
					newNodeName = nodeName.substring(0, indexHifen);
				}
				hashNodes.put(newNodeName, newNodeName);
			}

			for (String serverName : hashNodes.keySet()) {
				System.out.println(String.format("[%s] [%s]", serverName, hashNodes.get(serverName)));
			}

			Node[] nodes = hashNodes.values().toArray(new Node[0]);
			System.out.println(nodes.length);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
